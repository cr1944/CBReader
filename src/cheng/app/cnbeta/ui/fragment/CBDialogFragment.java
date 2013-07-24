package cheng.app.cnbeta.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import cheng.app.cnbeta.R;
import cheng.app.cnbeta.ui.activity.LauncherActivity;

public class CBDialogFragment extends DialogFragment {
    public static final String EXTRA_TYPE = "type";
    public static final int DIALOG_NONET_NOSD = 1;
    public static final int DIALOG_NONET = 2;

    public static CBDialogFragment newInstance(int type) {
        CBDialogFragment frag = new CBDialogFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_TYPE, type);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int type = getArguments().getInt(EXTRA_TYPE);
        switch (type) {
            case DIALOG_NONET_NOSD:
                return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.no_net_sd)
                .setPositiveButton(R.string.quit,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((LauncherActivity)getActivity()).quit();
                        }
                    }
                )
                .create();
            case DIALOG_NONET:
                return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.no_net)
                .setPositiveButton(R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((LauncherActivity)getActivity()).start();
                        }
                    })
                .setNegativeButton(R.string.quit,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((LauncherActivity)getActivity()).quit();
                        }
                    })
                .create();

        }
        return super.onCreateDialog(savedInstanceState);
    }
}
