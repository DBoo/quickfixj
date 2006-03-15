/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class quickfix_SessionSettings */

#ifndef _Included_quickfix_SessionSettings
#define _Included_quickfix_SessionSettings
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     quickfix_SessionSettings
 * Method:    create
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_quickfix_SessionSettings_create__
  (JNIEnv *, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    create
 * Signature: (Ljava/io/InputStream;)V
 */
JNIEXPORT void JNICALL Java_quickfix_SessionSettings_create__Ljava_io_InputStream_2
  (JNIEnv *, jobject, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    create
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_quickfix_SessionSettings_create__Ljava_lang_String_2
  (JNIEnv *, jobject, jstring);

/*
 * Class:     quickfix_SessionSettings
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_quickfix_SessionSettings_destroy
  (JNIEnv *, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    get
 * Signature: (Lquickfix/SessionID;)Lquickfix/Dictionary;
 */
JNIEXPORT jobject JNICALL Java_quickfix_SessionSettings_get__Lquickfix_SessionID_2
  (JNIEnv *, jobject, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    set
 * Signature: (Lquickfix/SessionID;Lquickfix/Dictionary;)V
 */
JNIEXPORT void JNICALL Java_quickfix_SessionSettings_set__Lquickfix_SessionID_2Lquickfix_Dictionary_2
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    get
 * Signature: ()Lquickfix/Dictionary;
 */
JNIEXPORT jobject JNICALL Java_quickfix_SessionSettings_get__
  (JNIEnv *, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    set
 * Signature: (Lquickfix/Dictionary;)V
 */
JNIEXPORT void JNICALL Java_quickfix_SessionSettings_set__Lquickfix_Dictionary_2
  (JNIEnv *, jobject, jobject);

/*
 * Class:     quickfix_SessionSettings
 * Method:    size
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_quickfix_SessionSettings_size
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif