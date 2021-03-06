package ai.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import ai.api.model.AIResponse;

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

public class AIServiceException extends Exception {

    @Nullable
    private final AIResponse aiResponse;

    public AIServiceException() {
        aiResponse = null;
    }

    public AIServiceException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);
        aiResponse = null;
    }

    public AIServiceException(final String detailMessage) {
        super(detailMessage);
        aiResponse = null;

    }

    public AIServiceException(@NonNull final AIResponse aiResponse) {
        super();
        this.aiResponse = aiResponse;
    }

    @Nullable
    public AIResponse getResponse() {
        return aiResponse;
    }

    @Override
    public String getMessage() {
        if (aiResponse != null
                && aiResponse.getStatus() != null) {

            final String errorDetails = aiResponse.getStatus().getErrorDetails();
            if (!TextUtils.isEmpty(errorDetails)) {
                return errorDetails;
            }
        }

        return super.getMessage();
    }
}
