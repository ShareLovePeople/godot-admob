def can_build(env, platform):
	return platform=="android" or platform=="iphone"

def configure(env):
	if (env['platform'] == 'android'):
		
		env.android_add_dependency("implementation 'com.android.support:support-v4:28.0.0'")
		env.android_add_dependency("implementation 'com.google.android.gms:play-services-ads:10.0.1'")
		
		env.android_add_java_dir("android")
		env.android_add_to_manifest("android/AndroidManifestChunk.xml")
		env.android_add_to_permissions("android/AndroidPermissionsChunk.xml")
		env.disable_module()
