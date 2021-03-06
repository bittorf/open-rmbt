/*******************************************************************************
 * Copyright 2013-2016 alladin-IT GmbH
 * Copyright 2013-2016 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.rtr.rmbt.android.terms;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import at.alladin.rmbt.android.R;
import at.rtr.rmbt.android.main.AppConstants;
import at.rtr.rmbt.android.main.RMBTMainActivity;
import at.rtr.rmbt.android.util.ConfigHelper;

public class RMBTCheckFragment extends Fragment
{
    public enum CheckLayoutType {
        ACCEPT_BUTTON,
        ACCEPT_AND_CANCEL_BUTTON
    }

	public enum CheckType {
		NDT("file:///android_res/raw/ndt_info.html", AppConstants.PAGE_TITLE_NDT_CHECK, 
				R.string.terms_ndt_header, R.string.terms_ndt_accept_text, false,
                CheckLayoutType.ACCEPT_BUTTON, true, new int[] {R.string.terms_ndt_accept_button}, false),
		LOOP_MODE("file:///android_res/raw/loop_mode_info.html", AppConstants.PAGE_TITLE_LOOP_MODE_CHECK, 
				R.string.terms_loop_mode_header, 0, true,
                CheckLayoutType.ACCEPT_AND_CANCEL_BUTTON, false,
                new int[] {R.string.terms_check_accept_button, R.string.terms_check_decline_button}, true),
        LOOP_MODE2("file:///android_res/raw/loop_mode_info2.html", AppConstants.PAGE_TITLE_LOOP_MODE_CHECK2,
                R.string.terms_loop_mode_header2,0, true,
                CheckLayoutType.ACCEPT_AND_CANCEL_BUTTON, false,
                new int[] {R.string.terms_check_accept_button, R.string.terms_check_decline_button}, true);

        private final String templateFile;
		private final String fragmentTag;
		private final boolean defaultIsChecked;
		private final int titleId;
		private final int textId;
        private final CheckLayoutType layoutType;
        private final boolean hasCheckbox;
        private final int[] buttonStringIds;
        private final boolean finishActivity;

        /**
         * Construct a new Check Fragment
         * @param templateFile URL of the HTML file to be displayed in the webview
         * @param fragmentTag
         * @param titleId id of the title to be displayed
         * @param textId id of the text to be displayed (deprecated)
         * @param defaultIsChecked checkbox checked by default (if contains a checkbox)
         * @param layoutType layout type (accept, accept and cancel)
         * @param hasCheckbox if a checkbox is contained
         * @param buttonStringIds string ids of the buttons
         * @param finishActivity true if activity should be finished in case of decline
         */
		CheckType(final String templateFile, final String fragmentTag, final int titleId,
                  final int textId, final boolean defaultIsChecked, final CheckLayoutType layoutType,
                  final boolean hasCheckbox, final int[] buttonStringIds, final boolean finishActivity) {
			this.templateFile = templateFile;
			this.fragmentTag = fragmentTag;
			this.titleId = titleId;
			this.textId = textId;
			this.defaultIsChecked = defaultIsChecked;
            this.layoutType = layoutType;
            this.hasCheckbox = hasCheckbox;
            this.buttonStringIds = buttonStringIds;
            this.finishActivity = finishActivity;
		}

		public String getTemplateFile() {
			return templateFile;
		}

		public String getFragmentTag() {
			return fragmentTag;
		}

		public int getTitleId() {
			return titleId;
		}

		public int getTextId() {
			return textId;
		}

		public boolean isDefaultIsChecked() {
			return defaultIsChecked;
		}

		public CheckLayoutType getLayoutType() {
            return layoutType;
        }

        public boolean hasCheckbox() {
            return hasCheckbox;
        }

        public int[] getButtonStringIds() {
            return buttonStringIds;
        }
    }
	
	private CheckType checkType;
	
    private CheckBox checkBox;
    
    boolean firstTime = true;

    private CheckType followedByType;

    public static RMBTCheckFragment newInstance(final CheckType checkType) {
        return RMBTCheckFragment.newInstance(checkType, null);
    }

    public static RMBTCheckFragment newInstance(final CheckType checkType, final CheckType followedBy) {
    	final RMBTCheckFragment f = new RMBTCheckFragment();
    	final Bundle bdl = new Bundle(1);
    	bdl.putSerializable("checkType", checkType);
        bdl.putSerializable("followedByType", followedBy);
        f.setArguments(bdl);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	checkType = (CheckType) getArguments().get("checkType");
        followedByType = (CheckType) getArguments().get("followedByType");
    }
    
    public CheckType getCheckType() {
		return checkType;
	}

	@Override
    public void onSaveInstanceState(final Bundle b)
    {
	    super.onSaveInstanceState(b);
	    if (checkBox != null)
	        b.putBoolean("isChecked", checkBox.isChecked());
    }
    
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
    {
        if (!(getActivity() instanceof RMBTMainActivity))
            firstTime = false;
        
        final View v = inflater.inflate(R.layout.ndt_check, container, false);
        if (!firstTime) {
            v.findViewById(R.id.termsNdtButtonBack).setVisibility(View.GONE);
        }

        final TextView textTitle = (TextView) v.findViewById(R.id.check_fragment_title);
        textTitle.setText(checkType.getTitleId());

        checkBox = (CheckBox) v.findViewById(R.id.ndtCheckBox);

        if (checkType.hasCheckbox()) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setText(checkType.getTextId());
            checkBox.setFocusable(true); //set focus on button so that checkbox can be selected on Android TV like devices (5-way-navigation)
            checkBox.setFocusableInTouchMode(true);
            checkBox.requestFocus();

            if (savedInstanceState != null) {
                checkBox.setChecked(savedInstanceState.getBoolean("isChecked"));
            } else {
                checkBox.setChecked(checkType.isDefaultIsChecked());
            }
        }
        else {
            // View.GONE This view is invisible, and it doesn't take any space for layout purposes.
            // View.INVISIBLE This view is invisible, but it still takes up space for layout purposes.
            // Layout shall ignore checkBox, thus GONE
            checkBox.setVisibility(View.GONE);
        }

        
        final Button buttonAccept = (Button) v.findViewById(R.id.termsNdtAcceptButton);
        buttonAccept.setText(checkType.getButtonStringIds()[0]);
        
        if (! firstTime && checkType.hasCheckbox())
        {    
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    buttonAccept.setEnabled(isChecked);
                }
            });

            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    buttonAccept.setEnabled(firstTime || checkBox.isChecked());
                }
            }, 500);
        }
        else if (firstTime || !checkType.hasCheckbox()) {
            buttonAccept.setEnabled(true);
        }
        
        final WebView wv = (WebView) v.findViewById(R.id.ndtInfoWebView);

        String url = checkType.getTemplateFile();

        //ndt terms may be loaded from URL
        if (checkType == CheckType.NDT) {
            final String ndtUrl = ConfigHelper.getTCNdtUrl(getActivity());
            if (ndtUrl != null) {
                url = ndtUrl;
            }
        }

        wv.loadUrl(url);
        
        buttonAccept.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                final FragmentActivity activity = getActivity();
                
                switch (checkType) {
                case NDT:
                    ConfigHelper.setNDT(activity, checkBox.isChecked());
                    ConfigHelper.setNDTDecisionMade(activity, true);                	
                	break;
                case LOOP_MODE2:
                	ConfigHelper.setLoopMode(activity, true);
                	break;
                }
                
                activity.getSupportFragmentManager().popBackStack(checkType.getFragmentTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
                
                if (firstTime && CheckType.NDT.equals(checkType)) {
                    ((RMBTMainActivity) activity).initApp(false);
                }
                else
                {
                    if (followedByType == null) {
                        final int result;
                        if (checkType.hasCheckbox()) {
                            result = checkBox.isChecked() ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
                        }
                        else {
                            result = Activity.RESULT_OK;
                        }

                        System.out.println("result = " + result);

                        getActivity().setResult(result);
                        getActivity().finish();
                    }
                    else {
                        ((RMBTTermsActivity) getActivity()).continueWorkflow(followedByType);
                    }
                }                
            }
        });

        final Button buttonBack = (Button) v.findViewById(R.id.termsNdtBackButton);

        if (checkType.getLayoutType() == CheckLayoutType.ACCEPT_AND_CANCEL_BUTTON) {
            v.findViewById(R.id.termsNdtButtonBack).setVisibility(View.VISIBLE);
            buttonBack.setText(checkType.getButtonStringIds()[1]);
        }

        buttonBack.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                getActivity().getSupportFragmentManager().popBackStack();
                if (checkType.finishActivity) {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    getActivity().finish();
                }
            }
        });
        
        return v;
    }

}