/*
 *  Copyright (c) 2006 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Created by August Mueller on Wed Feb 04 2005.
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ch_cyberduck_core_Proxy */

#ifndef _Included_ch_cyberduck_core_Proxy
#define _Included_ch_cyberduck_core_Proxy
#ifdef __cplusplus
extern "C" {
#endif
    JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_usePassiveFTPNative(JNIEnv *, jobject);
    JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_isHostExcludedNative(JNIEnv *, jobject, jstring);

	JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_isSOCKSProxyEnabledNative(JNIEnv *, jobject);
	JNIEXPORT jstring JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_getSOCKSProxyHostNative(JNIEnv *, jobject);
	JNIEXPORT jint JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_getSOCKSProxyPortNative(JNIEnv *, jobject);

	JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_isHTTPProxyEnabledNative(JNIEnv *, jobject);
	JNIEXPORT jstring JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_getHTTPProxyHostNative(JNIEnv *, jobject);
	JNIEXPORT jint JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_getHTTPProxyPortNative(JNIEnv *, jobject);

	JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_isHTTPSProxyEnabledNative(JNIEnv *, jobject);
	JNIEXPORT jstring JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_getHTTPSProxyHostNative(JNIEnv *, jobject);
	JNIEXPORT jint JNICALL Java_ch_cyberduck_core_SystemConfigurationProxy_getHTTPSProxyPortNative(JNIEnv *, jobject);
#ifdef __cplusplus
}
#endif
#endif

#import <Cocoa/Cocoa.h>
#import <Foundation/Foundation.h>
#import <SystemConfiguration/SystemConfiguration.h>

@interface Proxy : NSObject
{
	
}

+ (BOOL)usePassiveFTP;

+ (NSEnumerator*)getProxiesExceptionList;

+ (BOOL)isSimpleHostnameExcluded;

+ (BOOL)isSOCKSProxyEnabled;

+ (NSString *)getSOCKSProxyHost;

+ (NSNumber *)getSOCKSProxyPort;

+ (BOOL)isHTTPProxyEnabled;

+ (NSString *)getHTTPProxyHost;

+ (NSNumber *)getHTTPProxyPort;

+ (BOOL)isHTTPSProxyEnabled;

+ (NSString *)getHTTPSProxyHost;

+ (NSNumber *)getHTTPSProxyPort;

@end