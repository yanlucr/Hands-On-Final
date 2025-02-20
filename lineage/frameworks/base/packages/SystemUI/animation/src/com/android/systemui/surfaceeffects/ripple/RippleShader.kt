/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.systemui.surfaceeffects.ripple

import android.graphics.RuntimeShader
import android.util.Log
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import androidx.annotation.VisibleForTesting
import com.android.systemui.surfaceeffects.shaderutil.SdfShaderLibrary
import com.android.systemui.surfaceeffects.shaderutil.ShaderUtilLibrary

/**
 * Shader class that renders an expanding ripple effect. The ripple contains three elements:
 * 1. an expanding filled [RippleShape] that appears in the beginning and quickly fades away
 * 2. an expanding ring that appears throughout the effect
 * 3. an expanding ring-shaped area that reveals noise over #2.
 *
 * The ripple shader will default to the circle shape if not specified.
 *
 * Modeled after frameworks/base/graphics/java/android/graphics/drawable/RippleShader.java.
 */
class RippleShader(shaderShape: RippleShape = RippleShape.CIRCLE) :
    RuntimeShader(buildShader(shaderShape)) {

    // ADDED: Save the chosen shape so that we can later use it in property setters.
    private val shaderShape: RippleShape = shaderShape

    /** Shapes that the [RippleShader] supports. */
    enum class RippleShape {
        CIRCLE,
        ROUNDED_BOX,
        ELLIPSE,
        BORDER
    }
    // language=AGSL
    companion object {
        private val TAG = RippleShader::class.simpleName

        // Default fade in/ out values. The value range is [0,1].
        const val DEFAULT_FADE_IN_START = 0f
        const val DEFAULT_FADE_OUT_END = 1f

        const val DEFAULT_BASE_RING_FADE_IN_END = 0.1f
        const val DEFAULT_BASE_RING_FADE_OUT_START = 0.3f

        const val DEFAULT_SPARKLE_RING_FADE_IN_END = 0.1f
        const val DEFAULT_SPARKLE_RING_FADE_OUT_START = 0.4f

        const val DEFAULT_CENTER_FILL_FADE_IN_END = 0f
        const val DEFAULT_CENTER_FILL_FADE_OUT_START = 0f
        const val DEFAULT_CENTER_FILL_FADE_OUT_END = 0.6f

        const val RIPPLE_SPARKLE_STRENGTH: Float = 0.3f
        const val RIPPLE_DEFAULT_COLOR: Int = 0xffffffff.toInt()
        const val RIPPLE_DEFAULT_ALPHA: Int = 115 // full opacity is 255.

        //add in_battery for the border shader
        private const val SHADER_UNIFORMS =
            """
            uniform vec2 in_center;
            uniform vec2 in_size;
            uniform float in_cornerRadius;
            uniform float in_thickness;
            uniform float in_time;
            uniform float in_battery;
            uniform float in_distort_radial;
            uniform float in_distort_xy;
            uniform float in_fadeSparkle;
            uniform float in_fadeFill;
            uniform float in_fadeRing;
            uniform float in_blur;
            uniform float in_pixelDensity;
            layout(color) uniform vec4 in_color;
            uniform float in_sparkle_strength;
        """
        

        private const val SHADER_CIRCLE_MAIN =
            """
            vec4 main(vec2 p) {
                vec2 p_distorted = distort(p, in_time, in_distort_radial, in_distort_xy);
                float radius = in_size.x * 0.5;
                float sparkleRing = soften(circleRing(p_distorted-in_center, radius), in_blur);
                float inside = soften(sdCircle(p_distorted-in_center, radius * 1.25), in_blur);
                float sparkle = sparkles(p - mod(p, in_pixelDensity * 0.8), in_time * 0.00175)
                    * (1.-sparkleRing) * in_fadeSparkle;

                float rippleInsideAlpha = (1.-inside) * in_fadeFill;
                float rippleRingAlpha = (1.-sparkleRing) * in_fadeRing;
                float rippleAlpha = max(rippleInsideAlpha, rippleRingAlpha) * in_color.a;
                vec4 ripple = vec4(in_color.rgb, 1.0) * rippleAlpha;
                return mix(ripple, vec4(sparkle), sparkle * in_sparkle_strength);
            }
        """

        private const val SHADER_ROUNDED_BOX_MAIN =
            """
            vec4 main(vec2 p) {
                float sparkleRing = soften(roundedBoxRing(p-in_center, in_size, in_cornerRadius,
                    in_thickness), in_blur);
                float inside = soften(sdRoundedBox(p-in_center, in_size * 1.25, in_cornerRadius),
                    in_blur);
                float sparkle = sparkles(p - mod(p, in_pixelDensity * 0.8), in_time * 0.00175)
                    * (1.-sparkleRing) * in_fadeSparkle;

                float rippleInsideAlpha = (1.-inside) * in_fadeFill;
                float rippleRingAlpha = (1.-sparkleRing) * in_fadeRing;
                float rippleAlpha = max(rippleInsideAlpha, rippleRingAlpha) * in_color.a;
                vec4 ripple = vec4(in_color.rgb, 1.0) * rippleAlpha;
                return mix(ripple, vec4(sparkle), sparkle * in_sparkle_strength);
            }
        """

        private const val SHADER_ELLIPSE_MAIN =
            """
            vec4 main(vec2 p) {
                vec2 p_distorted = distort(p, in_time, in_distort_radial, in_distort_xy);

                float sparkleRing = soften(ellipseRing(p_distorted-in_center, in_size), in_blur);
                float inside = soften(sdEllipse(p_distorted-in_center, in_size * 1.2), in_blur);
                float sparkle = sparkles(p - mod(p, in_pixelDensity * 0.8), in_time * 0.00175)
                    * (1.-sparkleRing) * in_fadeSparkle;

                float rippleInsideAlpha = (1.-inside) * in_fadeFill;
                float rippleRingAlpha = (1.-sparkleRing) * in_fadeRing;
                float rippleAlpha = max(rippleInsideAlpha, rippleRingAlpha) * in_color.a;
                vec4 ripple = vec4(in_color.rgb, 1.0) * rippleAlpha;
                return mix(ripple, vec4(sparkle), sparkle * in_sparkle_strength);
            }
        """
        private const val SHADER_BORDER_MAIN =
            """
            
            const float pi2 = 1.5708;

            // Função SDF (se precisar dela para a máscara interna)
            float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
                // 'halfSize' representa as semi-extensões do retângulo centrado em (0,0).
                // Logo, p deve estar em um referencial cujo (0,0) é o centro do retângulo.
                // Exemplo: p - c, onde c é o centro da tela, se você quiser a forma centralizada.
                vec2 d = abs(p) - halfSize + vec2(r);
                float outside = length(max(d, vec2(0.0)));
                float inside = min(max(d.x, d.y), 0.0);
                return outside + inside - r;
            }

            vec4 main(vec2 p)
            {
                // Removemos p += in_center * 0.0; => p agora reflete o canto superior esquerdo.

                // Variáveis de tamanho
                float r = in_thickness;
                float w = in_size.x;
                float h = in_size.y;

                // Cálculo do perímetro (topLen, sideLen, etc.) permanece igual.
                float topLen = w - 2.0 * r;      
                float sideLen = h - 2.0 * r;     
                float arcLen = pi2 * r;          
                float totalP = 2.0 * (topLen + sideLen) + 4.0 * arcLen;

                float animatedLength = in_battery * totalP;

                // Cálculo da posição "t" (0 até totalP) ao longo da borda.
                // Aqui, p = (0,0) no canto sup. esquerdo e (w,h) no canto inf. direito.
                float t = -1.0;
                if (p.y < r && p.x >= r && p.x <= w - r) {
                    t = p.x - r;
                } else if (p.x > w - r && p.y < r) {
                    float dx = p.x - (w - r);
                    float dy = r - p.y;
                    float angle = acos(clamp(dy / r, 0.0, 1.0));
                    t = topLen + angle * r;
                } else if (p.x > w - r && p.y >= r && p.y <= h - r) {
                    t = topLen + arcLen + (p.y - r);
                } else if (p.x > w - r && p.y > h - r) {
                    float dx = (w - r) - p.x;
                    float dy = p.y - (h - r);
                    float angle = acos(clamp(dx / r, 0.0, 1.0));
                    t = topLen + arcLen + sideLen + angle * r;
                } else if (p.y > h - r && p.x >= r && p.x <= w - r) {
                    t = topLen + arcLen + sideLen + arcLen + ((w - r) - p.x);
                } else if (p.x < r && p.y > h - r) {
                    float dx = r - p.x;
                    float dy = p.y - (h - r);
                    float angle = acos(clamp(dx / r, 0.0, 1.0));
                    t = topLen + arcLen + sideLen + arcLen + topLen + angle * r;
                } else if (p.x < r && p.y >= r && p.y <= h - r) {
                    t = topLen + arcLen + sideLen + arcLen + topLen + arcLen + ((h - r) - p.y);
                } else if (p.x < r && p.y < r) {
                    float dx = r - p.x;
                    float dy = r - p.y;
                    float angle = acos(clamp(dy / r, 0.0, 1.0));
                    t = totalP - ((pi2 - angle) * r);
                }

                // fill = 1.0 se t estiver dentro do comprimento animado; 0.0 caso contrário.
                float fill = (t < 0.0) ? 0.0 : step(t, animatedLength);

                // Cor interpolada entre vermelho (0%) e verde (100%) de acordo com in_battery.
                vec3 borderColor = mix(vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0), in_battery);

                // Anti-aliasing:
                // Aqui, definimos que a "borda ideal" é [r, w-r] e [r, h-r]. Vamos medir distância.
                // dVec mede quanto p está fora desse retângulo interno.
                vec2 dVec = abs(p - vec2(w*0.5, h*0.5)) - vec2(w*0.5 - r, h*0.5 - r);
                float aa = 1.0 - smoothstep(0.0, r,
                    length(max(dVec, vec2(0.0))) + min(max(dVec.x, dVec.y), 0.0)
                );

                // Máscara interna (opcional):
                // Se quiser limpar o interior, podemos usar sdRoundedRect:
                // Precisamos que (0,0) seja o centro do retângulo => subtraímos (w/2, h/2).
                // halfSize = (w/2 - r, h/2 - r).
                float innerDist = sdRoundedRect(
                    p - vec2(w*0.5, h*0.5),     // desloca p para o centro
                    vec2(w*0.5 - r, h*0.5 - r), // half-size do retângulo interno
                    r                           // raio
                );
                // Se innerDist < 0 => p está dentro do retângulo. 
                // smoothstep(0,1,innerDist) => 0 dentro, 1 fora.
                float innerMask = smoothstep(0.0, 1.0, innerDist);

                return vec4(borderColor, fill * aa * innerMask);
            }
            """

        private const val CIRCLE_SHADER =
            SHADER_UNIFORMS +
                ShaderUtilLibrary.SHADER_LIB +
                SdfShaderLibrary.SHADER_SDF_OPERATION_LIB +
                SdfShaderLibrary.CIRCLE_SDF +
                SHADER_CIRCLE_MAIN
        private const val ROUNDED_BOX_SHADER =
            SHADER_UNIFORMS +
                ShaderUtilLibrary.SHADER_LIB +
                SdfShaderLibrary.SHADER_SDF_OPERATION_LIB +
                SdfShaderLibrary.ROUNDED_BOX_SDF +
                SHADER_ROUNDED_BOX_MAIN
        private const val ELLIPSE_SHADER =
            SHADER_UNIFORMS +
                ShaderUtilLibrary.SHADER_LIB +
                SdfShaderLibrary.SHADER_SDF_OPERATION_LIB +
                SdfShaderLibrary.ELLIPSE_SDF +
                SHADER_ELLIPSE_MAIN
        private const val BORDER_SHADER =
            SHADER_UNIFORMS +
                ShaderUtilLibrary.SHADER_LIB +
                SdfShaderLibrary.SHADER_SDF_OPERATION_LIB +
                SdfShaderLibrary.BORDER_SDF +
                SHADER_BORDER_MAIN

        private fun buildShader(rippleShape: RippleShape): String =
            when (rippleShape) {
                RippleShape.CIRCLE -> CIRCLE_SHADER
                RippleShape.ROUNDED_BOX -> ROUNDED_BOX_SHADER
                RippleShape.ELLIPSE -> ELLIPSE_SHADER
                RippleShape.BORDER -> BORDER_SHADER 
            }

        private fun subProgress(start: Float, end: Float, progress: Float): Float {
            // Avoid division by 0.
            if (start == end) {
                return if (progress > start) 1f else 0f
            }
            val min = Math.min(start, end)
            val max = Math.max(start, end)
            val sub = Math.min(Math.max(progress, min), max)
            return (sub - start) / (end - start)
        }

        private fun getFade(fadeParams: FadeParams, rawProgress: Float): Float {
            val fadeIn = subProgress(fadeParams.fadeInStart, fadeParams.fadeInEnd, rawProgress)
            val fadeOut = 1f - subProgress(fadeParams.fadeOutStart, fadeParams.fadeOutEnd, rawProgress)
            return Math.min(fadeIn, fadeOut)
        }

        private fun lerp(start: Float, stop: Float, amount: Float): Float {
            return start + (stop - start) * amount
        }

        // Copied from [Interpolators#STANDARD]. This is to remove dependency on AnimationLib.
        private val STANDARD: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    }

    /** Sets the center position of the ripple. */
    fun setCenter(x: Float, y: Float) {
        setFloatUniform("in_center", x, y)
    }

    /**
     * Blur multipliers for the ripple.
     *
     * It interpolates from [blurStart] to [blurEnd] based on the [progress]. Increase number to add more blur.
     */
    var blurStart: Float = 1.25f
    var blurEnd: Float = 0.5f

    /** Size of the ripple. */
    val rippleSize = RippleSize()

    /**
     * Linear progress of the ripple. Float value between [0, 1].
     *
     * Note that the progress here is expected to be linear without any curve applied.
     */
    var rawProgress: Float = 0.0f
        set(value) {
            field = value
            progress = STANDARD.getInterpolation(value)

            setFloatUniform("in_fadeSparkle", getFade(sparkleRingFadeParams, value))
            setFloatUniform("in_fadeRing", getFade(baseRingFadeParams, value))
            setFloatUniform("in_fadeFill", getFade(centerFillFadeParams, value))
        }

    /** Progress with Standard easing curve applied. */
    private var progress: Float = 0.0f
        set(value) {
            field = value

            // ADDED: If using the BORDER shader, always use the maximum size.
            if (shaderShape == RippleShape.BORDER && rippleSize.sizes.isNotEmpty()) {
                // Use the size from the last SizeAtProgress (i.e. maximum size).
                val maxSize = rippleSize.sizes.last()
                setFloatUniform("in_size", maxSize.width, maxSize.height)
                setFloatUniform("in_thickness", (maxSize.width * 0.07f).toFloat()) // Após testes de shader no AGSL
            } else {
                rippleSize.update(value)
                setFloatUniform("in_size", rippleSize.currentWidth, rippleSize.currentHeight)
                setFloatUniform("in_thickness", rippleSize.currentHeight * 0.5f)
                // Corner radius is always the minimum of the current width and height.
                setFloatUniform(
                    "in_cornerRadius",
                    Math.min(rippleSize.currentWidth, rippleSize.currentHeight)
                )
                setFloatUniform("in_blur", lerp(1.25f, 0.5f, value))
            }
            
        }

    /** Play time since the start of the effect. */
    var time: Float = 0.0f
        set(value) {
            field = value
            setFloatUniform("in_time", value)
        }

    /** A hex value representing the ripple color, in the format of ARGB */
    var color: Int = 0xffffff
        set(value) {
            field = value
            setColorUniform("in_color", value)
        }

    /**
     * Noise sparkle intensity. Expected value between [0, 1].
     * With 0 the ripple is smooth; with 1 it is fully grainy.
     */
    var sparkleStrength: Float = 0.0f
        set(value) {
            field = value
            setFloatUniform("in_sparkle_strength", value)
        }

    /** Distortion strength of the ripple. Expected value between 0 and 1. */
    var distortionStrength: Float = 0.0f
        set(value) {
            field = value
            setFloatUniform("in_distort_radial", 75 * rawProgress * value)
            setFloatUniform("in_distort_xy", 75 * value)
        }

    /**
     * Pixel density of the screen that the effects are rendered to.
     *
     * This value should come from [resources.displayMetrics.density].
     */
    var pixelDensity: Float = 1.0f
        set(value) {
            field = value
            setFloatUniform("in_pixelDensity", value)
        }

    /** Parameters used to fade in/out the sparkle ring. */
    val sparkleRingFadeParams =
        FadeParams(
            DEFAULT_FADE_IN_START,
            DEFAULT_SPARKLE_RING_FADE_IN_END,
            DEFAULT_SPARKLE_RING_FADE_OUT_START,
            DEFAULT_FADE_OUT_END
        )

    /**
     * Parameters used to fade in/out the base ring.
     *
     * Note that the shader draws the sparkle ring on top of the base ring.
     */
    val baseRingFadeParams =
        FadeParams(
            DEFAULT_FADE_IN_START,
            DEFAULT_BASE_RING_FADE_IN_END,
            DEFAULT_BASE_RING_FADE_OUT_START,
            DEFAULT_FADE_OUT_END
        )

    /** Parameters used to fade in/out the center fill. */
    val centerFillFadeParams =
        FadeParams(
            DEFAULT_FADE_IN_START,
            DEFAULT_CENTER_FILL_FADE_IN_END,
            DEFAULT_CENTER_FILL_FADE_OUT_START,
            DEFAULT_CENTER_FILL_FADE_OUT_END
        )

    /**
     * Parameters for the fade in/out of the ripple.
     *
     * For example:
     * SizeAtProgress(t= 0f, width= 0f, height= 0f), SizeAtProgress(t= 1f, width= maxWidth, height= maxHeight)
     */
    data class FadeParams(
        var fadeInStart: Float = DEFAULT_FADE_IN_START,
        var fadeInEnd: Float,
        var fadeOutStart: Float,
        var fadeOutEnd: Float = DEFAULT_FADE_OUT_END,
    )

    /**
     * Desired size of the ripple at a given progress.
     *
     * Example: SizeAtProgress(t=0f, width=0f, height=0f), SizeAtProgress(t=1f, width=maxWidth, height=maxHeight)
     */
    data class SizeAtProgress(
        var t: Float,
        var width: Float,
        var height: Float
    )

    /** Updates and stores the ripple size. */
    inner class RippleSize {
        @VisibleForTesting var sizes = mutableListOf<SizeAtProgress>()
        @VisibleForTesting var currentSizeIndex = 0
        @VisibleForTesting val initialSize = SizeAtProgress(0f, 0f, 0f)

        var currentWidth: Float = 0f
            private set
        var currentHeight: Float = 0f
            private set

        /**
         * Sets the max size of the ripple.
         *
         * Use this if the ripple shape changes linearly.
         */
        fun setMaxSize(width: Float, height: Float) {
            setSizeAtProgresses(initialSize, SizeAtProgress(1f, width, height))
        }

        /**
         * Sets the list of sizes.
         *
         * Note that this clears the existing sizes.
         */
        fun setSizeAtProgresses(vararg sizes: SizeAtProgress) {
            this.sizes.clear()
            currentSizeIndex = 0
            this.sizes.addAll(sizes)
            this.sizes.sortBy { it.t }
        }

        /**
         * Updates the current ripple size based on the progress.
         * Should be called when progress updates.
         */
        fun update(progress: Float) {
            val targetIndex = updateTargetIndex(progress)
            val prevIndex = Math.max(targetIndex - 1, 0)

            val targetSize = sizes[targetIndex]
            val prevSize = sizes[prevIndex]

            val subProgress = subProgress(prevSize.t, targetSize.t, progress)

            currentWidth = targetSize.width * subProgress + prevSize.width
            currentHeight = targetSize.height * subProgress + prevSize.height
        }

        private fun updateTargetIndex(progress: Float): Int {
            if (sizes.isEmpty()) {
                if (progress > 0f) {
                    Log.e(
                        TAG,
                        "Did you forget to set the ripple size? Use [setMaxSize] or [setSizeAtProgresses] before playing the animation."
                    )
                }
                setSizeAtProgresses(initialSize)
                return currentSizeIndex
            }

            var candidate = sizes[currentSizeIndex]

            while (progress > candidate.t) {
                currentSizeIndex = Math.min(currentSizeIndex + 1, sizes.size - 1)
                candidate = sizes[currentSizeIndex]
            }

            return currentSizeIndex
        }
    }
}
