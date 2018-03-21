package cn.intret.app.picgo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.jakewharton.rxbinding2.widget.RxTextView;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.ViewUtil;
import io.reactivex.Observable;


public class T9KeypadView extends LinearLayout implements
        OnClickListener, OnLongClickListener {
    private static final char DIAL_1_SECOND_MEANING = ' ';
    private static final char DIAL_X_SECOND_MEANING = ',';
    private static final char DIAL_0_SECOND_MEANING = '+';
    private static final char DIAL_J_SECOND_MEANING = ';';

    public Observable<CharSequence> getKeypadInputObservable() {
        return mDialpadInputObservable;
    }

    private Observable<CharSequence> mDialpadInputObservable;

    /**
     * Interface definition for a callback to be invoked when a
     * {@link T9KeypadView} is operated.
     */
    public interface OnT9KeypadInteractionHandler {
        void onKeypadAddCharacter(String addCharacter);

        void onKeypadDeleteCharacter(String deleteCharacter);

        void onKeypadInputTextChanged(String curCharacter);

        void onKeypadHide();
    }

    private Context mContext;
    /**
     * Inflate Custom T9 phone dialpad View hierarchy from the specified xml
     * resource.
     */
    private View mDialpadView; // this Custom View As the T9TelephoneDialpadView
    // of children
    private Button mTelephoneDialCloseBtn;
    private Button mDialDeleteBtn;
    private EditText mT9InputEt;
    private OnT9KeypadInteractionHandler mOnT9KeypadInteractionHandler = null;

    public T9KeypadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
        initData();
        initListener();
    }

    public void show() {
        ViewUtil.showView(this);
    }

    public void hide() {
        ViewUtil.hideView(this);
    }

    private void initData() {

    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDialpadView = inflater.inflate(R.layout.t9_keypad_layout, this);

        mTelephoneDialCloseBtn = mDialpadView.findViewById(R.id.telephone_dial_close_btn);
        mDialDeleteBtn = mDialpadView.findViewById(R.id.dial_delete_btn);
        mT9InputEt = mDialpadView.findViewById(R.id.dial_input_edit_text);
        mT9InputEt.setBackground(getContext().getResources().getDrawable(R.drawable.bg_empty));
        mT9InputEt.setCursorVisible(false);
    }

    private void initListener() {
        mTelephoneDialCloseBtn.setOnClickListener(this);
        mDialDeleteBtn.setOnClickListener(this);
        mDialDeleteBtn.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                //deleteAllDialCharacter();
                return true;
            }
        });

        /**
         * set click listener for button("0-9",'*','#')
         */
        for (int i = 0; i < 12; i++) {
            View v = mDialpadView.findViewById(R.id.dialNum1 + i);
            v.setOnClickListener(this);
        }

        mDialpadView.findViewById(R.id.num0).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num1).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num2).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num3).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num4).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num5).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num6).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num7).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num8).setOnClickListener(this);
        mDialpadView.findViewById(R.id.num9).setOnClickListener(this);
        mDialpadView.findViewById(R.id.nums).setOnClickListener(this);
        mDialpadView.findViewById(R.id.del).setOnClickListener(this);

        /**
         * set long click listener for button('1','*','0','#')
         * */
        View view1 = mDialpadView.findViewById(R.id.dialNum1);
        view1.setOnLongClickListener(this);

        View viewX = mDialpadView.findViewById(R.id.dialx);
        viewX.setOnLongClickListener(this);

        View viewO = mDialpadView.findViewById(R.id.dialNum0);
        viewO.setOnLongClickListener(this);

        View viewJ = mDialpadView.findViewById(R.id.dialj);
        viewJ.setOnLongClickListener(this);

        mDialpadInputObservable = RxTextView.textChanges(mT9InputEt)
                .doOnNext(charSequence -> {
                    String s = charSequence.toString();
                    if (mOnT9KeypadInteractionHandler != null) {
                        mOnT9KeypadInteractionHandler.onKeypadInputTextChanged(s);
                    }
                    mT9InputEt.setSelection(s.length());
                });

//        mT9InputEt.addTextChangedListener(new TextWatcher() {
//
//			@Override
//			public void onTextChanged(CharSequence s, int start, int before,
//                                      int count) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void beforeTextChanged(CharSequence s, int start, int count,
//                                          int after) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void afterTextChanged(Editable s) {
//				if (null != mOnT9KeypadInteractionHandler) {
//					String inputStr=s.toString();
//					mOnT9KeypadInteractionHandler.onKeypadInputTextChanged(inputStr);
//					mT9InputEt.setSelection(inputStr.length());
//
//					// Toast.makeText(mContext,
//					// "onKeypadInputTextChanged[" + s.toString() + "]",
//					// Toast.LENGTH_SHORT).show();
//				}
//			}
//		});

        mT9InputEt.setOnTouchListener(new OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // In order to prevent the soft keyboard pops up,but also can
                // not make EditText get focus.
                return true; // the listener has consumed the event
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.telephone_dial_close_btn:
                hideT9TelephoneDialpadView();
                if (null != mOnT9KeypadInteractionHandler) {
                    mOnT9KeypadInteractionHandler.onKeypadHide();
                }
                break;
            case R.id.dial_delete_btn:
                deleteAllDialCharacter();
                break;
            case R.id.dial_input_edit_text:

                break;
            case R.id.del:
                deleteSingleDialCharacter();
                break;
            case R.id.num0:
            case R.id.num1:
            case R.id.num2:
            case R.id.num3:
            case R.id.num4:
            case R.id.num5:
            case R.id.num6:
            case R.id.num7:
            case R.id.num8:
            case R.id.num9:
            case R.id.star:

            case R.id.dialNum0:
            case R.id.dialNum1:
            case R.id.dialNum2:
            case R.id.dialNum3:
            case R.id.dialNum4:
            case R.id.dialNum5:
            case R.id.dialNum6:
            case R.id.dialNum7:
            case R.id.dialNum8:
            case R.id.dialNum9:
            case R.id.dialx:
            case R.id.dialj:
                addSingleDialCharacter(v.getTag().toString());
                break;

            default:
                break;
        }

    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.dialNum1:
                addSingleDialCharacter(String.valueOf(DIAL_1_SECOND_MEANING));
                break;
            case R.id.dialx:
                addSingleDialCharacter(String.valueOf(DIAL_X_SECOND_MEANING));
                break;
            case R.id.dialNum0:
                addSingleDialCharacter(String.valueOf(DIAL_0_SECOND_MEANING));
                break;
            case R.id.dialj:
                addSingleDialCharacter(String.valueOf(DIAL_J_SECOND_MEANING));
                break;
            default:
                break;
        }
        return true;
    }

    public OnT9KeypadInteractionHandler getOnT9KeypadInteractionHandler() {
        return mOnT9KeypadInteractionHandler;
    }

    public void setOnT9KeypadInteractionHandler(
            OnT9KeypadInteractionHandler onT9KeypadInteractionHandler) {
        mOnT9KeypadInteractionHandler = onT9KeypadInteractionHandler;
    }

    public EditText getT9InputEt() {
        return mT9InputEt;
    }

    public void setT9InputEt(EditText t9InputEt) {
        mT9InputEt = t9InputEt;
    }

    public void deleteSingleDialCharacter() {
        String curInputStr = mT9InputEt.getText().toString();
        if (curInputStr.length() > 0) {
            String deleteCharacter = curInputStr.substring(
                    curInputStr.length() - 1, curInputStr.length());
            if (null != mOnT9KeypadInteractionHandler) {
                mOnT9KeypadInteractionHandler
                        .onKeypadDeleteCharacter(deleteCharacter);
            }

            String newCurInputStr = curInputStr.substring(0, curInputStr.length() - 1);
            mT9InputEt.setText(newCurInputStr);
            mT9InputEt.setSelection(newCurInputStr.length());
            if (TextUtils.isEmpty(newCurInputStr)) {
                ViewUtil.hideView(mDialDeleteBtn);
            } else {
                ViewUtil.showView(mDialDeleteBtn);
            }


        }
    }

    public void deleteAllDialCharacter() {
        String curInputStr = mT9InputEt.getText().toString();
        if (curInputStr.length() > 0) {
            String deleteCharacter = curInputStr.substring(0,
                    curInputStr.length());
            if (null != mOnT9KeypadInteractionHandler) {
                mOnT9KeypadInteractionHandler
                        .onKeypadDeleteCharacter(deleteCharacter);
            }
            mT9InputEt.setText("");
            ViewUtil.hideView(mDialDeleteBtn);
        }
    }

    private void addSingleDialCharacter(String addCharacter) {
        String preInputStr = mT9InputEt.getText().toString();
        if (!TextUtils.isEmpty(addCharacter)) {
            mT9InputEt.setText(preInputStr + addCharacter);
            mT9InputEt.setSelection(mT9InputEt.getText().length());
            if (null != mOnT9KeypadInteractionHandler) {
                mOnT9KeypadInteractionHandler.onKeypadAddCharacter(addCharacter);
            }
            ViewUtil.showView(mDialDeleteBtn);
        }

        // Toast.makeText(mContext, "addSingleDialCharacter[" + addCharacter +
        // "]",
        // Toast.LENGTH_SHORT).show();
    }

    public void showT9TelephoneDialpadView() {
        if (this.getVisibility() != View.VISIBLE) {
            this.setVisibility(View.VISIBLE);
        }
    }

    public void hideT9TelephoneDialpadView() {
        if (this.getVisibility() != View.GONE) {
            this.setVisibility(View.GONE);
        }
    }

    public int getT9TelephoneDialpadViewVisibility() {
        return this.getVisibility();
    }

    public String getT9Input() {
        return mT9InputEt.getText().toString();
    }

    public void clearT9Input() {
        mT9InputEt.setText("");
        mDialDeleteBtn.setVisibility(INVISIBLE);
    }

}
