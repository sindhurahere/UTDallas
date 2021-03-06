package ai.api.model;

/***********************************************************************************************************************
 * API.AI Android SDK - client-side libraries for API.AI
 * =================================================
 * <p/>
 * Copyright (C) 2015 by Speaktoit, Inc. (https://www.speaktoit.com)
 * https://www.api.ai
 * <p/>
 * **********************************************************************************************************************
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ***********************************************************************************************************************/

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Fulfillment implements Serializable {

    @SerializedName("speech")
    private String speech;

    public String getSpeech() {
        return speech;
    }

    public void setSpeech(final String speech) {
        this.speech = speech;
    }
}
