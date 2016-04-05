package dk.aau.sw808f16.datacollection.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import dk.aau.sw808f16.datacollection.R;

public class ConfirmSaveSelectionFragment extends DialogFragment {


  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setMessage(R.string.confirm_save_selection)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            ((SaveConfirmedCampaign) getParentFragment()).onConfirmedCampaignSave();
          }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
          }
        });
    // Create the AlertDialog object and return it
    return builder.create();
  }

  public interface SaveConfirmedCampaign {
    void onConfirmedCampaignSave();
  }

}