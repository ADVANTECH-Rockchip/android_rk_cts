/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Don't edit this file!  It is auto-generated by frameworks/rs/api/generate.sh.

#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)


float __attribute__((kernel)) testLgammaFloatFloat(float inV) {
    return lgamma(inV);
}

float2 __attribute__((kernel)) testLgammaFloat2Float2(float2 inV) {
    return lgamma(inV);
}

float3 __attribute__((kernel)) testLgammaFloat3Float3(float3 inV) {
    return lgamma(inV);
}

float4 __attribute__((kernel)) testLgammaFloat4Float4(float4 inV) {
    return lgamma(inV);
}
rs_allocation gAllocOutSignOfGamma;

float __attribute__((kernel)) testLgammaFloatIntFloat(float inV, unsigned int x) {
    int outSignOfGamma = 0;
    float out = lgamma(inV, &outSignOfGamma);
    rsSetElementAt_int(gAllocOutSignOfGamma, outSignOfGamma, x);
    return out;
}

float2 __attribute__((kernel)) testLgammaFloat2Int2Float2(float2 inV, unsigned int x) {
    int2 outSignOfGamma = 0;
    float2 out = lgamma(inV, &outSignOfGamma);
    rsSetElementAt_int2(gAllocOutSignOfGamma, outSignOfGamma, x);
    return out;
}

float3 __attribute__((kernel)) testLgammaFloat3Int3Float3(float3 inV, unsigned int x) {
    int3 outSignOfGamma = 0;
    float3 out = lgamma(inV, &outSignOfGamma);
    rsSetElementAt_int3(gAllocOutSignOfGamma, outSignOfGamma, x);
    return out;
}

float4 __attribute__((kernel)) testLgammaFloat4Int4Float4(float4 inV, unsigned int x) {
    int4 outSignOfGamma = 0;
    float4 out = lgamma(inV, &outSignOfGamma);
    rsSetElementAt_int4(gAllocOutSignOfGamma, outSignOfGamma, x);
    return out;
}
