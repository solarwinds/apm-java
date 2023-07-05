package com.appoptics.opentelemetry.instrumentation.guidewire;

import com.appoptics.opentelemetry.core.CustomTransactionNameDict;
import com.appoptics.opentelemetry.core.RootSpan;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class GuidewireTypeInstrumentation implements TypeInstrumentation {
    private final String basePackageName = "javax.servlet";

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed(basePackageName + ".Servlet");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return hasSuperType(namedOneOf(basePackageName + ".Filter", basePackageName + ".Servlet"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                namedOneOf("doFilter", "service")
                        .and(takesArgument(0, named(basePackageName + ".ServletRequest")))
                        .and(takesArgument(1, named(basePackageName + ".ServletResponse")))
                        .and(isPublic()),
                GuidewireTypeInstrumentation.class.getName() + "$GuidewireAdvice");
    }

    public static class GuidewireAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
                @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
            if ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
                service((HttpServletRequest) request);
            }
        }

        private static final Map<String, String> selectedParametersMap;

        public static void service(HttpServletRequest request) {
            Span currentSpan = Span.fromContext(Context.current());
            String traceId = currentSpan.getSpanContext().getTraceId();
            Logger logger = LoggerFactory.getLogger();

            logger.trace("GUIDEWIRE - Starting HttpServlet service method");
            logger.trace("GUIDEWIRE - browser character encoding before GW: " + request.getCharacterEncoding());
            logger.trace("GUIDEWIRE - Calling original HttpServlet.service method");

            logger.trace("GUIDEWIRE - browser character encoding after GW: " + request.getCharacterEncoding());

            try {
                Map<String, String[]> pMap = request.getParameterMap();
                boolean bWizardAndScreenCheckNeeded = false;
                boolean bWizardAndScreenFound = false;
                boolean bEventSourceValue = false;
                String pWizard = "";
                String pScreen = "";
                String eventSource = request.getParameter("eventSource");
                if (eventSource != null && !eventSource.isEmpty()) {
                    // Setting a boolean, so we don't have to do string functions on eventSource again later.
                    bEventSourceValue = true;

                    if (eventSource.contains("Wizard:Next_act")) {
                        //Setting a flag to activate additional logic to report wizard and screen name parameters and include them in the trans name
                        bWizardAndScreenCheckNeeded = true;
                    }
                }
                // Get all request parameter keys and add custom parameters if they are found in the list of known values.
                for (String pKey : pMap.keySet()) {
                    logger.trace("GUIDEWIRE - Processing request param: " + pKey);
                    String paramDisplayName = selectedParametersMap.get(pKey);

                    if (paramDisplayName != null) {
                        logger.trace("GUIDEWIRE - Found request param: " + pKey);
                        String[] pValue = request.getParameterValues(pKey);
                        if ((pValue != null) && (pValue.length > 0)) {
                            StringBuilder pValueString = new StringBuilder();
                            for (String pThisString : pValue) {
                                pValueString.append(" ").append(pThisString);
                            }
                            pValueString = new StringBuilder(pValueString.toString().trim());
                            if (pValueString.length() > 0) {
                                RootSpan.addAttribute(traceId, paramDisplayName, pValueString.toString());
                                logger.trace("GUIDEWIRE - adding attribute for request parameter: " + paramDisplayName + " = " + pValueString);

                            }
                        }
                    }

                    // Check if we need to capture wizard name and screen name.  Separate if statement to attempt performance improvement.
                    if ((bWizardAndScreenCheckNeeded) && (!bWizardAndScreenFound)) {
                        // Capture wizard name and screen name in case they are needed to set the transaction name.  They would appear in different parts of a single pKey value.  We only want to store this if we find both.
                        if ((pKey.contains("Wizard:")) && (pKey.contains("Screen:"))) {
                            String[] pKeyParts = pKey.split(":");
                            boolean bWizardFound = false;
                            boolean bScreenFound = false;
                            for (String pSplitKey : pKeyParts) {
                                if (pSplitKey.endsWith("Wizard")) {
                                    pWizard = pSplitKey.trim();
                                    bWizardFound = true;
                                }
                                if (pSplitKey.endsWith("Screen")) {
                                    pScreen = pSplitKey.trim();
                                    bScreenFound = true;
                                }
                                if (bWizardFound && bScreenFound) {
                                    bWizardAndScreenFound = true;
                                    break;
                                }
                            }
                        }
                    }

                }

                logger.trace("GUIDEWIRE - processing cookies");
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        String cName = cookie.getName();
                        if (cName != null && cName.startsWith("JSESSIONID")) {
                            RootSpan.addAttribute(traceId, cookie.getName(), cookie.getValue());
                            logger.trace(

                                    "GUIDEWIRE - adding attribute for JSESSIONID cookie: " + cookie.getName() + " = " +
                                            cookie.getValue());
                        }
                    }
                }

                // eventSource will be the transaction name unless it is _refresh_ and we have a good value for eventParam OR eventSource contains Wizard:Next_act and we captured good wizard and screen names.
                logger.trace("GUIDEWIRE - processing eventSource");
                if (bEventSourceValue) {
                    if (eventSource.equals("_refresh_")) {
                        String eventParam = request.getParameter("eventParam");
                        if (eventParam != null && !eventParam.isEmpty()) {
                            logger.trace("GUIDEWIRE - Setting eventParam to transaction name since eventSource was _refresh_: " + eventParam);
                            CustomTransactionNameDict.set(traceId, eventParam);
                        } else {
                            logger.trace("GUIDEWIRE - Setting eventSource to transaction name: " + eventSource);
                            CustomTransactionNameDict.set(traceId, eventSource);
                        }
                    } else {
                        if (bWizardAndScreenCheckNeeded && bWizardAndScreenFound) {
                            logger.trace("GUIDEWIRE - Setting Wizard Name and Screen name to transaction name since eventSource contained Wizard:Next_Act: " + pWizard + ":" + pScreen);
                            CustomTransactionNameDict.set(traceId, pWizard + ":" + pScreen);
                            RootSpan.addAttribute(traceId, "Wizard", pWizard);

                            RootSpan.addAttribute(traceId, "Screen", pScreen);
                        } else {
                            logger.trace("GUIDEWIRE - Setting eventSource to transaction name: " + eventSource);
                            CustomTransactionNameDict.set(traceId, eventSource);
                        }
                    }
                }


                String name = Thread.currentThread().getName();
                RootSpan.addAttribute(traceId, "ThreadName", name);

            } catch (Exception e) {
                logger.trace("GUIDEWIRE -- exception processing servlet service method: " + e.getMessage());
            }

        }

        static {
            LoggerFactory.getLogger().trace("GUIDEWIRE Initializing parameter map");
            selectedParametersMap = new HashMap<String, String>();
            //Starting your parameter key with % will trigger contains logic instead of expecting an exact match
            selectedParametersMap.put("eventSource", "eventSource");
            selectedParametersMap.put("eventParam", "eventParam");
            //Below entries are Nationwide specific: modify according to customer needs
            selectedParametersMap.put("SimpleClaimSearch:SimpleClaimSearchScreen:SimpleClaimSearchDV:ClaimNumber", "Claim Number");
            selectedParametersMap.put("SimpleClaimSearch:SimpleClaimSearchScreen:SimpleClaimSearchDV:PolicyNumber", "Policy Number");
            selectedParametersMap.put("SimpleClaimSearch:SimpleClaimSearchScreen:SimpleClaimSearchDV:FirstName", "First Name");
            selectedParametersMap.put("SimpleClaimSearch:SimpleClaimSearchScreen:SimpleClaimSearchDV:LastName", "Last Name");
            selectedParametersMap.put("SimpleClaimSearch:SimpleClaimSearchScreen:SimpleClaimSearchDV:CompanyName", "Organization Name");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:ClaimNumber", "Claim Number");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:PolicyNumber", "Policy Number");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:FirstName", "First Name");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:LastName", "Last Name");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:AssignedToGroup", "Assigned To Group");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:AssignedToUser", "Assigned To User");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:CreatedBy", "Created By");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:CatNumber", "CAT/STORM");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchRequiredInputSet:VinNumber", "VIN");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:lossStateActiveSearch", "Loss State");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:ClaimStatus", "Claim Status");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:nwPolicyType", "Policy Type");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:LossType", "Loss Type");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:DateSearch:DateSearchRangeValue", "Search For Date Since");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:DateSearch:DateSearchStartDate", "Search For Data From");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchOptionalInputSet:DateSearch:DateSearchEndDate", "Search For Data To");
            selectedParametersMap.put("ClaimSearch:ClaimSearchScreen:ClaimSearchDV:ClaimSearchAndResetInputSet:Search_act", "Search_act");
            selectedParametersMap.put("ClaimNewDocumentFromTemplateWorksheet:NewDocumentFromTemplateScreen:NewTemplateDocumentDV:CreateDocument", "Create Document From Template");
            selectedParametersMap.put("ClaimNewDocumentFromTemplateWorksheet:NewDocumentFromTemplateScreen:NewTemplateDocumentDV:CreateDocument_act", "Create Document From act");
            selectedParametersMap.put("ClaimNewDocumentFromTemplateWorksheet:NewDocumentFromTemplateScreen:NewTemplateDocumentDV:ViewLink_link", "ViewLink_link");
            selectedParametersMap.put("Login:LoginScreen:LoginDV:username", "User Name");
            selectedParametersMap.put("NewSubmission:NewSubmissionScreen:ProductSettingsDV:DefaultBaseState", "State");


            LoggerFactory.getLogger().trace("GUIDEWIRE Initialized parameter map: " + selectedParametersMap.size());
        }

    }


}
