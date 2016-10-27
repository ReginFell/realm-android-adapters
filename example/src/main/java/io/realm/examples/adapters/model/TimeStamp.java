/*
 * Copyright 2016 Realm Inc.
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
package io.realm.examples.adapters.model;

import io.realm.RealmObject;

public class TimeStamp extends RealmObject {

    public static final String TIMESTAMP = "timeStamp";

    private String timeStamp;

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        TimeStamp timeStamp1 = (TimeStamp) o;

        return getTimeStamp() != null ? getTimeStamp().equals(timeStamp1.getTimeStamp()) : timeStamp1.getTimeStamp() == null;

    }

    @Override
    public int hashCode() {
        return getTimeStamp() != null ? getTimeStamp().hashCode() : 0;
    }
}
