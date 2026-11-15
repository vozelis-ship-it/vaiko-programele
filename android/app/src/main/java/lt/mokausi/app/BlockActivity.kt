package lt.mokausi.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Rodoma, kai žaidimo laikas baigėsi. „Atgal“ veda į pagrindinį ekraną, ne į žaidimą. */
class BlockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = { v: Int -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt() }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4f46e5")); setPadding(dp(32), dp(32), dp(32), dp(32))
        }
        root.addView(TextView(this).apply { text = "⏰"; textSize = 96f; gravity = Gravity.CENTER })
        root.addView(TextView(this).apply {
            text = "Laikas baigėsi.\nIšspręsk užduotis ir užsidirbk daugiau!"
            textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(32))
        })
        root.addView(Button(this).apply {
            text = "Į užduotis"; textSize = 24f; isAllCaps = false; setTextColor(Color.parseColor("#4f46e5"))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(20).toFloat() }
            setPadding(dp(40), dp(18), dp(40), dp(18))
            setOnClickListener {
                startActivity(Intent(this@BlockActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        })
        setContentView(root)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
