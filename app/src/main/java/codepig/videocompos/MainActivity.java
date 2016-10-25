package codepig.videocompos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private Button filterDemo,composDemo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化各面板
        findView();
    }
    private void findView(){
        filterDemo=(Button) findViewById(R.id.filterDemo);
        composDemo=(Button) findViewById(R.id.composDemo);

        filterDemo.setOnClickListener(clickBtn);
        composDemo.setOnClickListener(clickBtn);
    }

    private View.OnClickListener clickBtn = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.filterDemo:
                    Intent intent=new Intent(getApplication(), gpuImageFilter.class);
//                    intent.putExtra("vid_key", "");
                    startActivity(intent);
                    break;
                case R.id.composDemo:
                    Intent intent2=new Intent(getApplication(), composDemo.class);
                    startActivity(intent2);
                    break;
                default:
                    break;
            }
        }
    };
}