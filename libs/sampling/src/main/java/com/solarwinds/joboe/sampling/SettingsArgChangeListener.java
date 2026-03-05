/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.joboe.sampling;

import lombok.Getter;

/**
 * Listens to change of {@link SettingsArg} from {@link Settings}
 *
 * <p>This only gets notified when the value has been changed.
 *
 * <p>Take note that changing from null value to non-null and vice versa are considered as change
 * too
 *
 * @author pluk
 * @param <T>
 */
@Getter
public abstract class SettingsArgChangeListener<T> {
  private final SettingsArg<T> type;
  private T lastValue;

  public SettingsArgChangeListener(SettingsArg<T> type) {
    this.type = type;
  }

  public final void onValue(T value) {
    boolean changed;

    if (lastValue != null) {
      changed = !type.areValuesEqual(lastValue, value);
    } else {
      changed = (value != null);
    }

    if (changed) {
      lastValue = value;
      onChange(value);
    }
  }

  protected abstract void onChange(T newValue);
}
