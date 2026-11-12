//! JNI entry points.
//!
//! Keep this module limited to marshalling between the JVM and Rust. Domain
//! logic belongs in `lib.rs` so it stays testable without an Android runtime.

use jni::objects::JObject;
use jni::sys::jint;
use jni::JNIEnv;

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
