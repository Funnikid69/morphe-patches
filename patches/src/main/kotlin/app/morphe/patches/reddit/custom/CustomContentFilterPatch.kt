package app.morphe.patches.reddit.custom

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.reddit.misc.extension.sharedExtensionPatch

@Suppress("unused")
val customContentFilterPatch = bytecodePatch(
    name = "Custom keyword and image filter",
    description = "Filters configured keywords, emojis, Snoo flag assets, and banner/avatar images."
) {
    dependsOn(sharedExtensionPatch)
    compatibleWith("com.reddit.frontpage")

    execute {
        // Hook OkHttp Request.Builder.url(String) to redirect blocked Snoo/banner asset URLs
        okhttpUrlFingerprint.method.addInstruction(
            0,
            """
            invoke-static { p1 }, Lapp/morphe/extension/reddit/ContentFilterExtension;->shouldBlockAssetUrl(Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :allow
            const-string p1, "https://www.redditstatic.com/avatars/defaults/v2/avatar_default_0.png"
            :allow
            """.trimIndent()
        )
    }
}
