package com.appoptics.opentelemetry.instrumentation;

import com.solarwinds.joboe.core.EventValueConverter;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.SQLData;

public class JdbcEventValueConverter extends EventValueConverter {
  public static final int STRING_VALUE_MAX_LENGTH = 256;
  private static final JdbcEventValueConverter SINGLETON =
      new JdbcEventValueConverter(STRING_VALUE_MAX_LENGTH);

  public static Object convert(Object rawValue) {
    return SINGLETON.convertToEventValue(rawValue);
  }

  private JdbcEventValueConverter(int maxValueLength) {
    super(maxValueLength);

    // do not reference the sql classes directly. Since JDK 9+, those classes are no longer
    // accessible from bootstrap classloader, which is the class loader that loads all our
    // agent classes. Making reference to classes not from `java.base` module will throw
    // `NoClassDefFoundError`
    try {
      registerSpecialTypes("java.sql.Blob", new BlobParameterHandler());
      registerSpecialTypes("java.sql.Clob", new ClobParameterHandler());
      registerSpecialTypes("java.io.InputStream", new ClassNameParameterHandler());
      registerSpecialTypes("java.io.Reader", new ClassNameParameterHandler());
      registerSpecialTypes("java.sql.Array", new ArrayParameterHandler());
      registerSpecialTypes("java.sql.Ref", new RefParameterHandler());
    } catch (ClassNotFoundException e) {
      logger.info(
          "Cannot load value class for JDBC event value converter, JDBC instrumentation might not report proper values for some cases : "
              + e.getMessage());
    }

    // add support of 1.6+ classes, special handling since we have to be 1.5 compatible
    try {
      registerSpecialTypes("java.sql.RowId", new ToStringParameterHandler());
      registerSpecialTypes(
          "java.sql.SQLXML", new SqlXmlParameterHandler()); // TODO getString too costly?
      registerSpecialTypes("java.sql.NClob", new ClobParameterHandler());
      registerSpecialTypes("java.sql.SQLData", new SqlDataParameterHandler());
    } catch (ClassNotFoundException e) {
      logger.info(
          "Running java 1.5 or earlier version, would not track RowId nor SQLXML (1.6 classes) in PreparedStatement instrumentation");
    }
  }

  private void registerSpecialTypes(String className, Converter<?, ?> converter)
      throws ClassNotFoundException {
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    Class<?> targetClass;
    if (systemClassLoader != null) {
      targetClass = systemClassLoader.loadClass(className);
    } else {
      targetClass = Class.forName(className);
    }

    EXPECTED_SPECIAL_TYPES.put(targetClass, converter);
  }

  /**
   * Clob parameter handler, displays the type and the length (bytes designated) of the Clob object
   *
   * @author Patson Luk
   */
  private static class ClobParameterHandler extends Converter<Clob, String> {
    @Override
    protected String getValue(Clob parameter) {
      try {
        return "(Clob " + parameter.length() + " Bytes)";
      } catch (Exception e) {
        logger.warn("Failed to retrieve length of the Clob object: " + e.getMessage());
        return "(Clob)";
      }
    }
  }

  /**
   * Blob parameter handler, displays the type and the length (bytes designated) of the Blob object
   *
   * @author Patson Luk
   */
  private static class BlobParameterHandler extends Converter<Blob, String> {
    @Override
    protected String getValue(Blob parameter) {
      try {
        return "(Blob " + parameter.length() + " Bytes)";
      } catch (Exception e) {
        logger.warn("Failed to retrieve length of the Blob object: " + e.getMessage());
        return "(Blob)";
      }
    }
  }

  /**
   * Ref parameter handler. Display the baseTypeName of the Ref parameter
   *
   * @author Patson Luk
   */
  private static class RefParameterHandler extends Converter<Ref, String> {
    @Override
    protected String getValue(Ref parameter) {
      try {
        return "(Ref " + parameter.getBaseTypeName() + ")";
      } catch (Exception e) {
        logger.warn("Failed to retrieve base type name of the Ref object: " + e.getMessage());
        return "(Ref)";
      }
    }
  }

  /**
   * Array parameter handler. Do not do special handling as accessing the array might involve data
   * loading
   *
   * @author Patson Luk
   */
  private static class ArrayParameterHandler extends Converter<Object, String> {
    @Override
    protected String getValue(Object parameter) {
      return "(Array)";
    }
  }

  /**
   * SQLData(since java 1.6) handler, take note that it is OK to reference SQLData here as this
   * handler would not be loaded for 1.5
   *
   * @author Patson Luk
   */
  private static class SqlDataParameterHandler extends Converter<SQLData, String> {
    @Override
    protected String getValue(SQLData parameter) {
      try {
        return "(SQLData " + parameter.getSQLTypeName() + ")";
      } catch (Exception e) {
        logger.warn(e.getMessage());
        return "(SQLData unknown type)";
      }
    }
  }

  /**
   * SQLXML(since java 1.6) handler, take note that it is OK to reference SQLData here as this
   * handler would not be loaded for 1.5
   *
   * @author Patson Luk
   */
  private static class SqlXmlParameterHandler extends Converter<Object, String> {
    @Override
    protected String getValue(Object parameter) {
      return "(SQLXML)";
    }
  }
}
