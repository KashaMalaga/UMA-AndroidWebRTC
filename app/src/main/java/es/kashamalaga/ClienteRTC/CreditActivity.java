package es.kashamalaga.ClienteRTC;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * Created by Kasha on 05/04/2015.
 */
public class CreditActivity extends Activity {
     Scroller myscroll = null;
        TextView tvData = null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            setContentView(R.layout.activity_creditos);

            tvData = (TextView)findViewById(R.id.textview);

            myscroll = new Scroller(CreditActivity.this,
                    new LinearInterpolator());

            tvData.setScroller(myscroll);
            Scroll();


            Thread thread = new Thread()
            {
                @Override
                public void run() {
                    try {
                        while(true) {
                            Log.i("App", "Thread Run");
                            sleep(1000);
                            CreditActivity.this.runOnUiThread(compScroll);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();
        }



        private Runnable compScroll = new Runnable() {

            @Override
            public void run() {
                if(!myscroll.computeScrollOffset())
                {
                    Log.i("App", "Scroll Again");
                    Scroll();
                }

            }
        };

        public void Scroll() {
            tvData.setText(R.string.txtCreditos);
            int length = tvData.getLineCount();
            myscroll.startScroll(0, 0, 0, 500,10000);

        }

    @Override
    public void onBackPressed()
        {
           Intent intentar = new Intent(this, MainActivity.class);
            startActivity(intentar);
            finish();

        }

}





