/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
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
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

#import "Keychain.h"
#import <Security/Security.h>
#import <SecurityInterface/SFCertificateTrustPanel.h>
#import <SecurityInterface/SFChooseIdentityPanel.h>
#import <JavaNativeFoundation/JNFString.h>

#import "EMKeychainProxy.h"
#import "EMKeychainItem.h"

SecCertificateRef CreateCertificateFromData(JNIEnv *env, jbyteArray jCertificate);
NSArray* CreateCertificatesFromData(JNIEnv * env, jobjectArray jCertificateChain);

OSStatus CreateSSLClientPolicy(SecPolicyRef* policy);
OSStatus CreatePolicy(const CSSM_OID* policy_OID,
                      void* option_data,
                      size_t option_length,
                      SecPolicyRef* policy);

SecProtocolType convertToSecProtocolType(JNIEnv *env, jstring jProtocol)
{
    if([JNFJavaToNSString(env, jProtocol) isEqualTo: @"ftp"]) {
        return kSecProtocolTypeFTP;
    }
    if([JNFJavaToNSString(env, jProtocol) isEqualTo: @"ftps"]) {
        return kSecProtocolTypeFTPS;
    }
    if([JNFJavaToNSString(env, jProtocol) isEqualTo: @"sftp"]) {
        return kSecProtocolTypeSSH;
    }
    if([JNFJavaToNSString(env, jProtocol) isEqualTo: @"http"]) {
        return kSecProtocolTypeHTTP;
    }
    if([JNFJavaToNSString(env, jProtocol) isEqualTo: @"https"]) {
        return kSecProtocolTypeHTTPS;
    }
    return kSecProtocolTypeAny;
}

JNIEXPORT jstring JNICALL Java_ch_cyberduck_core_Keychain_getInternetPasswordFromKeychain(JNIEnv *env, jobject this, jstring jProtocol, 
																						  jint port, jstring jService,jstring jUsername) {
	
	EMInternetKeychainItem *keychainItem = [[EMKeychainProxy sharedProxy] internetKeychainItemForServer:JNFJavaToNSString(env, jService)
																						   withUsername:JNFJavaToNSString(env, jUsername)
																								   path:nil 
																								   port:port 
																							   protocol:convertToSecProtocolType(env, jProtocol)];
    return (*env)->NewStringUTF(env, [[keychainItem password] UTF8String]);
}

JNIEXPORT jstring JNICALL Java_ch_cyberduck_core_Keychain_getPasswordFromKeychain(JNIEnv *env, jobject this, jstring jService, jstring jUsername) {
	EMGenericKeychainItem *keychainItem = [[EMKeychainProxy sharedProxy] genericKeychainItemForService:JNFJavaToNSString(env, jService)
																						  withUsername:JNFJavaToNSString(env, jUsername)];
    return (*env)->NewStringUTF(env, [[keychainItem password] UTF8String]);
}

JNIEXPORT void JNICALL Java_ch_cyberduck_core_Keychain_addInternetPasswordToKeychain(JNIEnv *env, jobject this, jstring jProtocol, jint port, 
																					 jstring jService, jstring jUsername, jstring jPassword) {
	[[EMKeychainProxy sharedProxy] addInternetKeychainItemForServer:JNFJavaToNSString(env, jService)
													   withUsername:JNFJavaToNSString(env, jUsername)
														   password:JNFJavaToNSString(env, jPassword) 
															   path:nil 
															   port:port
														   protocol:convertToSecProtocolType(env, jProtocol)];
}

JNIEXPORT void JNICALL Java_ch_cyberduck_core_Keychain_addPasswordToKeychain(JNIEnv *env, jobject this, jstring jService, jstring jUsername, jstring jPass)  {
	[[EMKeychainProxy sharedProxy] addGenericKeychainItemForService:JNFJavaToNSString(env, jService) 
													   withUsername:JNFJavaToNSString(env, jUsername)
														   password:JNFJavaToNSString(env, jPass)];
}

jbyteArray GetCertData(JNIEnv *env, SecCertificateRef certificateRef) {
	CSSM_DATA cssmData;
	OSStatus err = SecCertificateGetData(certificateRef, &cssmData);
	if(err != noErr) {
		return NULL;
	}
	jbyteArray jb;
	jb=(*env)->NewByteArray(env, cssmData.Length);
	(*env)->SetByteArrayRegion(env, jb, 0, cssmData.Length, (jbyte *)cssmData.Data);
	return jb;
}

JNIEXPORT jbyteArray Java_ch_cyberduck_core_Keychain_chooseCertificateNative(JNIEnv *env, jobject this,
                                                                             jobjectArray jIssuers, jstring jHostname, jstring jPrompt) {
	//NSArray *identities = CreateCertificatesFromData(env, jCertificates);
    NSMutableArray *identities = [NSMutableArray array];
    SecIdentitySearchRef search;
    SecIdentitySearchCreate(NULL, CSSM_KEYUSE_SIGN, &search);
    SecIdentityRef identity;
    while (SecIdentitySearchCopyNext(search, &identity) == noErr) {
        [identities addObject: (id)identity];
        CFRelease( identity );
    }
    CFRelease(search);
	SFChooseIdentityPanel *panel = [[SFChooseIdentityPanel alloc] init];
    [panel setShowsHelp:NO];
    [panel setDomain:JNFJavaToNSString(env, jHostname)];
    [panel setAlternateButtonTitle:NSLocalizedString(@"Disconnect", @"")];
	[panel setInformativeText:JNFJavaToNSString(env, jPrompt)];
	// Create an SSL policy ref configured for client cert evaluation.
	SecPolicyRef policy;
	if (CreateSSLClientPolicy(&policy) == noErr) {
		[panel setPolicies:(id)policy];
		CFRelease(policy);
	}
	NSInteger result = [panel runModalForIdentities:identities message:nil];
	[panel release];
	if(result == NSOKButton) {
		SecIdentityRef identity = [panel identity];
		OSStatus err;
		SecCertificateRef certificateRef;
		err = SecIdentityCopyCertificate(identity, &certificateRef);
		if(err != noErr) {
			return NULL;
		}
		jbyteArray jb = GetCertData(env, certificateRef);
		return jb;
	}
	return NULL;
}

OSStatus CreateSSLClientPolicy(SecPolicyRef* policy) {
	SecPolicySearchRef search;
	OSStatus err = SecPolicySearchCreate(CSSM_CERT_X_509v3, &CSSMOID_APPLE_TP_SSL, NULL, &search);
	if (err) {
		return err;
	}
	err = SecPolicySearchCopyNext(search, policy);
	CFRelease(search);
	if (err) {
		return err;
	}
	CSSM_APPLE_TP_SSL_OPTIONS ssloptions = {
		CSSM_APPLE_TP_SSL_OPTS_VERSION,
		0,
		NULL,
		CSSM_APPLE_TP_SSL_CLIENT
	};
	CSSM_DATA options_data = {
		.Length = sizeof(ssloptions),
		.Data = (void*)&ssloptions
	};
	err = SecPolicySetValue(*policy, &options_data);
	if (err) {
		CFRelease(*policy);
		return err;
	}
	return noErr;
}

SecCertificateRef CreateCertificateFromData(JNIEnv *env, jbyteArray jCertificate) {
	OSStatus err;
	jbyte *certByte = (*env)->GetByteArrayElements(env, jCertificate, NULL);
	// Creates a certificate object based on the specified data, type, and encoding.
	NSData *certData = [NSData dataWithBytes:certByte length:(*env)->GetArrayLength(env, jCertificate)];
	(*env)->ReleaseByteArrayElements(env, jCertificate, certByte, 0);
	CSSM_DATA *cssmData = NULL;
	if (certData) {
		cssmData = (CSSM_DATA*)malloc(sizeof(CSSM_DATA));
		cssmData->Length = [certData length];
		cssmData->Data = (uint8*)malloc(cssmData->Length);
		[certData getBytes:(char*)(cssmData->Data)];
	}
	SecCertificateRef certificateRef = NULL;
	err = SecCertificateCreateFromData(cssmData, CSSM_CERT_X_509v3, CSSM_CERT_ENCODING_DER, &certificateRef);
	if(cssmData) {
		free(cssmData->Data);
		free(cssmData);
	}
	if(err != noErr) {
		NSLog(@"Error creating certificate from data");
		return NULL;;
	}
	return certificateRef;
}

NSArray* CreateCertificatesFromData(JNIEnv *env, jobjectArray jCertificates) {
    NSMutableArray *result = [NSMutableArray arrayWithCapacity:(*env)->GetArrayLength(env, jCertificates)];
	int i;
    for(i = 0; i < (*env)->GetArrayLength(env, jCertificates); i++) {
        jbyteArray jCertificate = (jbyteArray)(*env)->GetObjectArrayElement(env, jCertificates, i);
		SecCertificateRef ref = CreateCertificateFromData(env, jCertificate);
		if(NULL == ref) {
            NSLog(@"Error creating certificate from ASN.1 DER");
			continue;
		}
        [result addObject:(id)ref];
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_Keychain_isTrustedNative(JNIEnv *env, jobject this, jstring jHostname, jobjectArray jCertificates) {
	OSStatus err;
	NSArray *certificates = CreateCertificatesFromData(env, jCertificates);
	// Adds a certificates to a keychain.
	int i;
	for(i = 0; i < [certificates count]; i++) {
        SecCertificateRef certificateRef = (SecCertificateRef)[certificates objectAtIndex:i];
        err = SecCertificateAddToKeychain(certificateRef, NULL);
        switch(err)
        {
            case errSecDuplicateItem:
                // The function returns errSecDuplicateItem and does not add another copy to the keychain.
                // The function looks at the certificate data, not at the certificate object, to
                // determine whether the certificate is a duplicate. It considers two certificates to be
                // duplicates if they have the same primary key attributes.
                break;
            default:
                NSLog(@"Error adding certificate to Keychain");
                break;
        }
    }
	// Creates a search object for finding policies.
	SecPolicySearchRef searchRef = NULL;
	err = SecPolicySearchCreate(CSSM_CERT_X_509v3, &CSSMOID_APPLE_TP_SSL, NULL, &searchRef);
	if(err != noErr) {
        NSLog(@"Error creating policy");
		return FALSE;
	}
	// Retrieves a policy object for the next policy matching specified search criteria.
	SecPolicyRef policyRef = NULL;
	err = SecPolicySearchCopyNext(searchRef, &policyRef);
	if(err != noErr) {
        NSLog(@"Error retrieving policy");
		if(searchRef) {
			CFRelease(searchRef);
		}
		return FALSE;
	}
	if(searchRef) {
		CFRelease(searchRef);
	}
	if(!policyRef) {
	    return FALSE;
	}
	NSString *hostname = JNFJavaToNSString(env, jHostname);
	// Returns NULL if the receiver cannot be losslessly converted to encoding.
	const char *cHostname = [hostname cStringUsingEncoding:NSASCIIStringEncoding];
	if(!cHostname) {
        NSLog(@"Error adding hostname to SSL options");
	    return FALSE;
	}
	CSSM_APPLE_TP_SSL_OPTIONS ssloptions = {
		.Version = CSSM_APPLE_TP_SSL_OPTS_VERSION,
		.ServerNameLen = strlen(cHostname),
		.ServerName = cHostname,
		.Flags = 0
	};
	CSSM_DATA customCssmData = {
		.Length = sizeof(ssloptions),
		.Data = (uint8*)&ssloptions
	};
	err = SecPolicySetValue(policyRef, &customCssmData);
	if(err != noErr) {
        NSLog(@"Error setting policy for evaluating trust");
		if(policyRef) {
			CFRelease(policyRef);
		}
		return FALSE;
	}
	// Creates a trust management object based on certificates and policies.
	SecTrustRef trustRef = NULL;
	err = SecTrustCreateWithCertificates((CFArrayRef)certificates, policyRef, &trustRef);
	if(err != noErr) {
        NSLog(@"Error creating trust");
		if(policyRef) {
			CFRelease(policyRef);
		}
		return FALSE;
	}
	SecTrustResultType trustResult;
	err = SecTrustEvaluate(trustRef, &trustResult);
	if(err != noErr) {
        NSLog(@"Error evaluating trust");
		if(policyRef) {
			CFRelease(policyRef);
		}
		if(trustRef) {
			CFRelease(trustRef);
		}
		return FALSE;
	}
	// kSecTrustResultProceed -> trust ok, go right ahead
	// kSecTrustResultConfirm -> trust ok, but user asked (earlier) that you check with him before proceeding
	// kSecTrustResultDeny -> trust ok, but user previously said not to trust it anyway
	// kSecTrustResultUnspecified -> trust ok, user has no particular opinion about this
	// kSecTrustResultRecoverableTrustFailure -> trust broken, perhaps argue with the user
	// kSecTrustResultFatalTrustFailure -> trust broken, user can't fix it
	// kSecTrustResultOtherError -> something failed weirdly, abort operation
	// kSecTrustResultInvalid -> logic error; fix your program (SecTrust was used incorrectly)
	switch(trustResult) {
		case kSecTrustResultProceed:
			// Accepted by user keychain setting explicitly
			if(policyRef) {
				CFRelease(policyRef);
			}
			if(trustRef) {
				CFRelease(trustRef);
			}
			return TRUE;
		case kSecTrustResultUnspecified:
		    // See http://developer.apple.com/qa/qa2007/qa1360.html
		    // Unspecified means that the user never expressed any persistent opinion about
		    // this certificate (or any of its signers). Either this is the first time this certificate
		    // has been encountered (in these circumstances), or the user has previously dealt with it
		    // on a one-off basis without recording a persistent decision. In practice, this is what
		    // most (cryptographically successful) evaluations return.
		    // If the certificate is invalid kSecTrustResultUnspecified can never be returned.
			if(policyRef) {
				CFRelease(policyRef);
			}
			if(trustRef) {
				CFRelease(trustRef);
			}
			return TRUE;
		default:
			break;
	}
	CFArrayRef certChain;
	CSSM_TP_APPLE_EVIDENCE_INFO *statusChain;
	err = SecTrustGetResult(trustRef, &trustResult, &certChain, &statusChain);
//       CSSM_CERT_STATUS_EXPIRED            = 0x00000001,
//       CSSM_CERT_STATUS_NOT_VALID_YET      = 0x00000002,
//       CSSM_CERT_STATUS_IS_IN_INPUT_CERTS  = 0x00000004,
//       CSSM_CERT_STATUS_IS_IN_ANCHORS      = 0x00000008,
//       CSSM_CERT_STATUS_IS_ROOT            = 0x00000010,
//       CSSM_CERT_STATUS_IS_FROM_NET        = 0x00000020,
	SFCertificateTrustPanel *panel = [[SFCertificateTrustPanel alloc] init];
	if(err == noErr) {
		if ([panel respondsToSelector:@selector(setInformativeText:)]) {
			if((statusChain->StatusBits & CSSM_CERT_STATUS_EXPIRED) == CSSM_CERT_STATUS_EXPIRED) {
				[panel setInformativeText:[NSString stringWithFormat:NSLocalizedStringFromTable(@"The certificate for this server has expired. You might be connecting to a server that is pretending to be \u201c%@\u201d which could put your confidential information at risk. Would you like to connect to the server anyway?", @"Keychain", @""), hostname]];
			}
			else if((statusChain->StatusBits & CSSM_CERT_STATUS_NOT_VALID_YET) == CSSM_CERT_STATUS_NOT_VALID_YET) {
				[panel setInformativeText:[NSString stringWithFormat:NSLocalizedStringFromTable(@"The certificate for this server is not yet valid. You might be connecting to a server that is pretending to be \u201c%@\u201d which could put your confidential information at risk. Would you like to connect to the server anyway?", @"Keychain", @""), hostname]];
			}
			else if((statusChain->StatusBits & CSSM_CERT_STATUS_IS_ROOT) == CSSM_CERT_STATUS_IS_ROOT
			            && (statusChain->StatusBits & CSSM_CERT_STATUS_IS_IN_ANCHORS) != CSSM_CERT_STATUS_IS_IN_ANCHORS) {
				[panel setInformativeText:[NSString stringWithFormat:NSLocalizedStringFromTable(@"The certificate for this server was signed by an unknown certifying authority. You might be connecting to a server that is pretending to be \u201c%@\u201d which could put your confidential information at risk. Would you like to connect to the server anyway?", @"Keychain", @""), hostname]];
			}
			else if((statusChain->StatusBits & CSSM_CERT_STATUS_IS_IN_ANCHORS) != CSSM_CERT_STATUS_IS_IN_ANCHORS) {
				[panel setInformativeText:[NSString stringWithFormat:NSLocalizedStringFromTable(@"The certificate for this server is invalid. You might be connecting to a server that is pretending to be \u201c%@\u201d which could put your confidential information at risk. Would you like to connect to the server anyway?", @"Keychain", @""), hostname]];
			}
		}
	}
	if([panel respondsToSelector:@selector(setAlternateButtonTitle:)]) {
		[panel setAlternateButtonTitle:NSLocalizedString(@"Disconnect", @"")];
	}
	if([panel respondsToSelector:@selector(setPolicies:)]) {
		[panel setPolicies:(id)policyRef];
	}
	if([panel respondsToSelector:@selector(setShowsHelp:)]) {
		[panel setShowsHelp:YES];
	}
	// Displays a modal panel that shows the results of a certificate trust evaluation and
	// that allows the user to edit trust settings.
	int result = [panel runModalForTrust:trustRef message:nil];
	[panel release];
	if(policyRef) {
		CFRelease(policyRef);
	}
	if(trustRef) {
		CFRelease(trustRef);
	}
	if(NSOKButton == result) {
		return TRUE;
	}
    return FALSE;
}

JNIEXPORT jboolean JNICALL Java_ch_cyberduck_core_Keychain_displayCertificatesNative(JNIEnv *env, jobject this, jobjectArray jCertificates) {
	NSArray *certificates = CreateCertificatesFromData(env, jCertificates);
	SFCertificatePanel *panel = [[SFCertificatePanel alloc] init];
	if([panel respondsToSelector:@selector(setShowsHelp:)]) {
		[panel setShowsHelp:NO];
	}
	NSInteger result = [panel runModalForCertificates:certificates showGroup:YES];
	if(result == NSOKButton) {
		return TRUE;
	}
	return FALSE;
}