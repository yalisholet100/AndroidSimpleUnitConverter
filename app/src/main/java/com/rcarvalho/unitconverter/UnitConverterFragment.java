package com.rcarvalho.unitconverter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;

/**
 Created by Roger Carvalho for RDC Media Ltd. on 13/07/15. This code can freely be used, amended,
 distributed and sold for any purpose desired, but should credit RDC Media Ltd. within the notes.

 This a flexible multi-purpose unit converter. It imports any measurable unit profile from
 strings.xml and allows users to easily convert a given unit to another
 */
public class UnitConverterFragment extends Fragment {

    //The decimal precision provided by the app can be changed by changing the number here
    String decimalPrecision = "%.4f";

    /*
    These variables hold all possible profiles and measurable units. The array lists hold
    all the units that belong to a given profile and their respective multipliers to convert them
    amongst each other. The BaseUnit object holds the currently selected profile's data
    */
    private final ArrayList<String> profiles = new ArrayList<>();
    private final ArrayList<String> units = new ArrayList<>();
    private final ArrayList<Double> unitValues = new ArrayList<>();
    private BaseUnit activeUnits;

    /*
    These variables hold all the interactive UI elements in use by this app
     */
    EditText input;
    Spinner profile;
    Spinner baseUnit;
    Spinner resultUnit;
    Button convert;
    TextView resultLabel;

    public UnitConverterFragment()
    //Constructor, not required in this context
    {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    /*
    As soon as Android loads the activity, the app will import all available unit conversion
    profiles and load up the first profile for conversion
     */
    {
        super.onActivityCreated(savedInstanceState);

        //load up reference data from XML files and link the interactive UI elements
        linkUI();
        loadProfiles();

        /*
        Select the first profile to load units from. Any profile should have at least 2 units
        declared in strings.xml, as string arrays with the naming convention 'units<profile name>'
        for the unit names and 'units<profile name>Values' for the multipliers. (At least one
        multiplier should have the value 1)
         */
        int xmlUnitNamesReference = getStringArrayResourceID("units" + this.profiles.get(0));
        int xmlUnitValuesReference = getStringArrayResourceID("units" + this.profiles.get(0)
                + "Values");

        if (xmlUnitNamesReference == 0 || xmlUnitValuesReference == 0)
        /*
        This means the XML contains incomplete profiles, as Android could not find an array for
        either the names or the multipliers of the first profile. This means the app cannot load
        up the units, so it exits with a warning message
        */
        {
            showDialog(getResources().getString(R.string.errorMsgBoxTitle),
                    getResources().getString(R.string.unitsAndOrValuesNotFoundError), true);
        }
        else
        //The unit name and value references array was found in strings.xml, continue loading up
        {
            /*
            Find out if the profile selected has a base number (that should be added and subtracted
            upon applying the multiplier)
            */
            int xmlUnitBaseReference = getStringArrayResourceID("units" + this.profiles.get(0)
                    +"Base");

            //Load up the units in the BaseUnit object to be used for future conversion
            activeUnits = createBaseUnit(xmlUnitNamesReference, xmlUnitValuesReference,
                    xmlUnitBaseReference);

            /*
            Check if activeUnits was correctly created. If not, the app will have shown a dialogue
            box and is waiting for the user to exit
            */
            if (activeUnits != null)
            {

                //load the units into the input and output spinners
                setupSpinner(activeUnits, baseUnit, 0);
                setupSpinner(activeUnits, resultUnit, 1);

                /*
                Setup listeners for user selection of profiles, units or number entry. Whenever the user
                selects any entry in any of the spinners, types a number in the text field or presses
                the convert button, the app will either load up different units (in case of of the
                profile selection spinner) or try to convert based on the selection.
                */
                setupProfileListener();
                setupBaseUnitSpinnerListener();
                setupResultUnitSpinnerListener();
                setupButtonListener();
                setupTextInputListener();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unit_converter, container, false);
    }


    private void convert(double amount, String inputAmount, int baseIndex, int resultIndex,
                         boolean messageBox)
    /*
    This method converts a given amount from any given unit to another given unit. You need to
    provide it with an amount (as both a double and a String, so the method knows with what
    precision the amount was entered). You also need to provide the unit index number of the amount
    and what unit you want to convert the amount into (these are the positions of the units in the
    array of 'units<profile name>Values' in strings.xml). You can make this method provide a popup
    window confirming the conversion if desired
    */
    {
        //Load the amount into the BaseUnit object
        activeUnits.setValue(amount, baseIndex);

        //Get the return amount
        double result = activeUnits.convert(resultIndex);

        //Prepare output
        String outputAmount;
        if ((result - Math.floor(result)) > 0.0000001)
        //The output has decimal values, so apply the decimal precision requested in the class
        {
            outputAmount = String.format(decimalPrecision, result);
        }
        else
        //the output has no decimal values, so truncate/round
        {
            outputAmount = String.format("%.0f", result);
        }
        String message = inputAmount + " " + activeUnits.getUnitNames()[baseIndex] + " " +
                getResources().getString(R.string.isEqualTo)+ " " +
                outputAmount + " " + activeUnits.getUnitNames()[resultIndex];

        //Show output in the UI
        resultLabel.setText(message);

        if(messageBox)
        //Show output in a pop up dialogue if requested
        {
            showDialog(getResources().getString(R.string.conversionMsgBoxTitle), message, false);
        }
    }

    private BaseUnit createBaseUnit(int xmlUnitNamesReference, int xmlUnitValuesReference,
                                    int xmlBaseUnitReference)
    /*
    This method creates a new BaseUnit object based on a provided referenceIndex of strings in
    strings.xml
    */
    {
        //Load up all the names and multipliers listed in strings.xml
        String[] units = getResources().getStringArray(xmlUnitNamesReference);
        String[] unitBaseValuesStrings = getResources().getStringArray(xmlUnitValuesReference);

        //Ensure that the unit names and values arrays are of identical length and bigger than 2
        if (units.length == unitBaseValuesStrings.length && units.length > 1)
        {

            //Prepare an array of doubles to convert the multipliers found
            double[] unitBaseValues = new double[unitBaseValuesStrings.length];

            for (int i = 0; i < unitBaseValuesStrings.length; i++) {
                try
                {
                    //Convert multiplier to double
                    unitBaseValues[i] = Double.parseDouble(unitBaseValuesStrings[i]);

                }
                catch (NumberFormatException e)
                {
                    /*
                    If a multiplier could not be parsed to double, the app cannot continue, as it
                    will not be able to perform calculations.
                    */
                    showDialog(getResources().getString(R.string.errorMsgBoxTitle),
                            getResources().getString(R.string.nonParsableUnitValueError), true);
                }
            }

            /*
            Find out whether to build the base unit with or without a base number (that will be
            added and subtracted when applying a multiplier)
            */
            if (xmlBaseUnitReference != 0)
            {
                String[] baseNumber = getResources().getStringArray(xmlBaseUnitReference);

                try
                {
                    /*
                    Convert the base number to double and return a new BaseUnit with this base
                    number
                    */
                    double baseNumberValue = Double.parseDouble(baseNumber[0]);
                    return new BaseUnit(units, unitBaseValues, 0, baseNumberValue);

                }
                catch (NumberFormatException e)
                {
                    /*
                    If a base number could not be parsed to double, the app cannot continue, as it
                    will not be able to perform calculations.
                    */
                    showDialog(getResources().getString(R.string.errorMsgBoxTitle),
                            getResources().getString(R.string.nonParsableUnitValueError), true);
                }
                /*
                This should never be called, either the app will exit through an error dialogue or
                the BaseUnit has already been returned above.
                */
                return null;
            }
            else
            {
                //Return a new base unit with 0 as base number
                return new BaseUnit(units, unitBaseValues, 0, 0);
            }
        }
        else if (units.length < 2)
        //The app tried to load a profile with less than 2 units. This could never work, so exit
        {
            showDialog(getResources().getString(R.string.errorMsgBoxTitle),
                    getResources().getString(R.string.notEnoughUnitsError), true);
            return null;
        }
        else
        //The number of unit names and unit multipliers don't match. This threatens integrity
        {
            showDialog(getResources().getString(R.string.errorMsgBoxTitle),
                    getResources().getString(R.string.unitsAndUnitValuesDontMatchError), true);
            return null;
        }
    }

    private int getStringArrayResourceID(String resourceIDString)
    //This method returns the ID of a given resource string array in strings.xml
    {
        return getResources().getIdentifier(resourceIDString, "array",
                this.getActivity().getPackageName());
    }

    private void linkUI()
    //This method links any interactive UI elements to the code
    {
        input=(EditText) getActivity(). findViewById(R.id.input);
        profile = (Spinner) getActivity(). findViewById(R.id.spinnerCategory);
        baseUnit = (Spinner) getActivity(). findViewById(R.id.spinnerUnitsBase);
        resultUnit = (Spinner) getActivity(). findViewById(R.id.spinnerUnitsResult);
        resultLabel = ((TextView) getActivity(). findViewById (R.id.txtResult));
        convert = (Button) getActivity(). findViewById(R.id.btnConvert);
    }

    private void loadProfiles()
    //This method loads all conversion profiles listed in strings.xml into an array property
    {
        String[] profiles = getResources().getStringArray(R.array.profiles);
        for (String profile : profiles)
        {
            this.profiles.add(profile);
        }
    }

    private void setupBaseUnitSpinnerListener()
    //When the user has manually selected a given unit in the base spinner, perform conversion
    {
        baseUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                try {
                    //store the value the user wants to convert
                    double amount = Double.parseDouble(input.getText().toString());

                    //perform a conversion without a confirmation dialogue
                    convert(amount, input.getText().toString(), position,
                            resultUnit.getSelectedItemPosition(), false);
                } catch (NumberFormatException e) {
                    /*
                    If the user has not entered a number,revert to the default state, wait
                    for the user to do so
                    */
                    resultLabel.setText(getResources().getString(R.string.defaultResult));
                }
            }

            @Override
            //When the user hasn't selected any unit
            public void onNothingSelected(AdapterView<?> parentView) {
                //Revert to the default state, wait for the user to enter data
                resultLabel.setText(getResources().getString(R.string.defaultResult));
            }
        });
    }

    private void setupButtonListener()
    //When the user taps the 'Convert' button, perform a conversion with a pop up confirmation.
    {
        convert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //store the value the user wants to convert
                    double amount = Double.parseDouble(input.getText().toString());

                    //perform a conversion with a confirmation dialogue
                    convert(amount, input.getText().toString(), baseUnit.getSelectedItemPosition(),
                            resultUnit.getSelectedItemPosition(), true);

                } catch (NumberFormatException e) {
                    //If the user has not entered a number, inform them of this fact
                    showDialog(getResources().getString(R.string.noticeMsgBoxTitle),
                            getResources().getString(R.string.noNumberError), false);
                }

            }
        });
    }

    private void setupProfileListener()
    //When the user selects a profile, reload the unit spinners
    {

        profile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //reload spinners
                int xmlUnitNamesReference = getStringArrayResourceID("units" +
                        profiles.get(position));
                int xmlUnitValuesReference = getStringArrayResourceID("units" +
                        profiles.get(position) + "Values");

                //Check if the profiles have their required reference data in strings.xml
                if (xmlUnitNamesReference != 0 && xmlUnitValuesReference != 0) {
                    /*
                    Find out if the profile selected has a base number (that should be added and
                    subtracted upon applying the multiplier)
                    */
                    int xmlUnitBaseReference = getStringArrayResourceID("units" +
                            profiles.get(position) + "Base");

                    //Load up the units in the BaseUnit object to be used for future conversion
                    activeUnits = createBaseUnit(xmlUnitNamesReference, xmlUnitValuesReference,
                            xmlUnitBaseReference);

                     /*
                    Check if activeUnits was correctly created. If not, the app will have shown
                    a dialogue box and is waiting for the user to exit
                    */
                    if (activeUnits != null) {
                        /*
                        Load the units into the input and output spinners and set them to the first
                        and second unit available respectively
                         */
                        setupSpinner(activeUnits, baseUnit, 0);
                        setupSpinner(activeUnits, resultUnit, 1);
                    }


                } else {
                    /*
                    Without valid reference data, the app will crash. Therefore give an error
                    message and close if any data is not parsable
                     */
                    showDialog(getResources().getString(R.string.errorMsgBoxTitle),
                            getResources().getString(R.string.nonParsableUnitValueError), true);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });
    }

    private void setupResultUnitSpinnerListener()
    //When the user has manually selected a given unit in the result spinner, perform conversion
    {
        resultUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id)
            {
                try
                {
                    //store the value the user wants to convert
                    double amount = Double.parseDouble(input.getText().toString());

                    //perform a conversion without a confirmation dialogue
                    convert(amount, input.getText().toString(), baseUnit.getSelectedItemPosition(),
                            position, false);
                }
                catch (NumberFormatException e)
                {
                    /*
                    If the user has not entered a number,revert to the default state, wait
                    for the user to do so
                    */
                    resultLabel.setText(getResources().getString(R.string.defaultResult));

                }
            }

            @Override
            //When the user hasn't selected any unit
            public void onNothingSelected(AdapterView<?> parentView) {

                //Revert to the default state, wait for the user to enter data
                resultLabel.setText(getResources().getString(R.string.defaultResult));
            }

        });
    }

    private void setupSpinner(BaseUnit units, Spinner spinner, int defaultSelection)
    //This method loads up a given spinner with all available units in a BaseUnit object
    {

        //Load entries from the BaseUnit array into a spinner
        String[]unitNames = activeUnits.getUnitNames();
        ArrayAdapter<String> spinnerArrayAdapter;

        spinnerArrayAdapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_item, unitNames);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        //Select whatever item in the list the caller wanted as default selection
        spinner.setSelection(defaultSelection);
    }

    private void setupTextInputListener()
    /*
    When the user pressed enter or the 'done' button on the virtual keyboard after entering
    data, perform conversion
    */
    {

        input.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE))
                {
                    try
                    {
                        //store the value the user wants to convert
                        double amount = Double.parseDouble(input.getText().toString());

                        //perform a conversion without a confirmation dialogue
                        convert(amount, input.getText().toString(),
                                baseUnit.getSelectedItemPosition(),
                                resultUnit.getSelectedItemPosition(), false);

                        return false;

                    }
                    catch (NumberFormatException e)
                    {

                        /*
                        If the user has not entered a number,revert to the default state, wait
                        for the user to do so
                        */
                        resultLabel.setText(getResources().getString(R.string.defaultResult));
                        return false;
                    }
                }
                return false;
            }
        });
    }

    private void showDialog(String title, String message, boolean terminateApp)
    /*
    This method spawns a dialog box with a Close button. It can either terminate the app, or simply
    dismiss when pressing this button
    */
    {
        //Setup dialog
        AlertDialog ad = new AlertDialog.Builder(this.getActivity()).create();
        ad.setCancelable(false);
        ad.setTitle(title);
        ad.setMessage(message);

        if (terminateApp)
        //Close the app upon pressing the Close button
        {
            ad.setButton(getResources().getString(R.string.msgBoxCloseButtonTitle),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    System.exit(0);
                }
            });
        }
        else
        //Dismiss the dialogue upon pressing the Close button
        {
            ad.setButton(getResources().getString(R.string.msgBoxCloseButtonTitle),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
        }
        ad.show();
    }
}