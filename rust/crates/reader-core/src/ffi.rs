//! JNI entry points.
//!
//! Keep this module limited to marshalling between the JVM and Rust. Domain
//! logic belongs in `lib.rs` so it stays testable without an Android runtime.

use jni::objects::{JObject, JString};
use jni::sys::{jint, jobjectArray, jstring};
use jni::JNIEnv;

fn read_string(env: &mut JNIEnv<'_>, value: &JString<'_>) -> Option<String> {
    env.get_string(value).ok().map(Into::into)
}

fn to_string_array(env: &mut JNIEnv<'_>, items: &[String]) -> jobjectArray {
    let class = match env.find_class("java/lang/String") {
        Ok(class) => class,
        Err(_) => return std::ptr::null_mut(),
    };
    let array = match env.new_object_array(items.len() as jint, &class, JObject::null()) {
        Ok(array) => array,
        Err(_) => return std::ptr::null_mut(),
    };

    for (index, item) in items.iter().enumerate() {
        let value = match env.new_string(item) {
            Ok(value) => value,
            Err(_) => return std::ptr::null_mut(),
        };
        if env
            .set_object_array_element(&array, index as jint, &value)
            .is_err()
        {
            return std::ptr::null_mut();
        }
    }

    array.into_raw()
}

/// Implements `com.universalreader.app.core.NativeBridge.coreVersion()`.
///
/// `NativeBridge` is a Kotlin `object`, so the receiver is an instance
/// (`JObject`), not a static class (`JClass`).
#[no_mangle]
pub extern "system" fn Java_com_universalreader_app_core_NativeBridge_coreVersion(
    _env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jint {
    crate::core_version() as jint
}

/// Implements `NativeBridge.normalizeParagraphs(text)`.
#[no_mangle]
pub extern "system" fn Java_com_universalreader_app_core_NativeBridge_normalizeParagraphs<
    'local,
>(
    mut env: JNIEnv<'local>,
    _this: JObject<'local>,
    text: JString<'local>,
) -> jobjectArray {
    let Some(text) = read_string(&mut env, &text) else {
        return std::ptr::null_mut();
    };
    let paragraphs = crate::normalize_paragraphs(&text);
    to_string_array(&mut env, &paragraphs)
}

/// Implements `NativeBridge.ttsChunks(text, maxChars)`.
#[no_mangle]
pub extern "system" fn Java_com_universalreader_app_core_NativeBridge_ttsChunks<'local>(
    mut env: JNIEnv<'local>,
    _this: JObject<'local>,
    text: JString<'local>,
    max_chars: jint,
) -> jobjectArray {
    let Some(text) = read_string(&mut env, &text) else {
        return std::ptr::null_mut();
    };
    let chunks = crate::tts_chunks(&text, max_chars.max(0) as usize);
    to_string_array(&mut env, &chunks)
}

/// Implements `NativeBridge.canonicalizeUrl(url)`. Returns `null` for invalid
/// or non-http(s) URLs.
#[no_mangle]
pub extern "system" fn Java_com_universalreader_app_core_NativeBridge_canonicalizeUrl<'local>(
    mut env: JNIEnv<'local>,
    _this: JObject<'local>,
    url: JString<'local>,
) -> jstring {
    let Some(url) = read_string(&mut env, &url) else {
        return std::ptr::null_mut();
    };
    match crate::canonicalize_url(&url) {
        Some(canonical) => env
            .new_string(canonical)
            .map(|value| value.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        None => std::ptr::null_mut(),
    }
}
