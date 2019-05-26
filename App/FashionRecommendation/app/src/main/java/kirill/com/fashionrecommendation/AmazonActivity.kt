package kirill.com.fashionrecommendation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_amazon.*

class AmazonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amazon)

        val query = intent
            ?.extras
            ?.getCharSequence("query")
            ?.split('\n')
            ?.map { it.split(':')[0] }
            ?.filter { it.isNotEmpty() }
            ?.joinToString(" ")
            ?.replace(" ", "+")
            ?: "Nike"
        //webViewAmazon.settings.loadWithOverviewMode = true;
        //webViewAmazon.settings.useWideViewPort = true;
        /*webViewAmazon.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url)
                return true
            }
        }*/
        webViewAmazon.loadUrl("https://www.amazon.com/s?k=$query")
    }
}
