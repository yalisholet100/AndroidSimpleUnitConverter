# AndroidSimpleUnitConverter
This is a simple unit converter app for Android. You can load up any convertible units you'd like using XML.

The app uses a very simple user interface, which works both in landscape and portrait. It supports API 16 and up (Jelly Bean).

# Structure
The most important files of this repository are:

- app/src/main/java/com/rcarvalho/unitconverter/UnitConverterFragment.java (holds the main source code, which is executed when the app runs)
- app/src/main/java/com/rcarvalho/unitconverter/BaseUnit.java (holds the code that manages baseunits, which contain all information and logic around converting a given profile of units amongst eachother
- app/src/main/res/values/strings.xml (holds all localized strings used in the app, and also contains the actual profiles of units that are supported. You can freely add and remove profiles for conversion in this file, the app will automatically load these. Currently the only locale implemented is US English, but this can easily be extended to any number of languages.
- app/src/main/res/layout/ and app/src/main/res/layout-land/ (these folders contain the UI used)

This project was created using Android Studio. To compile, simply load the file UnitConverter.iml in Android studio and run. Edit the strings.xml file to support the measures of unit conversion you would like and update the UI where you see fit.

# Unit conversion profiles

All units that can be converted amongst eachother are stored in a so-called conversion profile. The app allows users to pick a given profile, which then allows them to enter a number that will be converted amongst the units in that profile.

 To load a profile of convertible units, simply open strings.xml and add the name of the profile under the array "profiles". Then create 2 new string arrays named "units<profile name>" (under which you list all names of the units within this profile), and "units<profileName>Values" (under which you list all of the multipliers to convert the units amongst each other).

Any profile added should have at least 2 items, and 1 of the items should have a multiplier of 1, to serve as a reference to convert the items freely amongst each other.

For example, if I want to convert apples to apple pies, and I know 1 apple pie requires 3 apples, I would enter the following:

1. Under <string-array name="profiles">, add: <item>ApplesToPies</item>
2. Then, right underneath the last string array, add:
    <string-array name="unitsApplesToPies">
        <item>Apples</item>
        <item>Pies</item>
    </string-array>
    <string-array name="unitsApplesToPiesValues">
        <item>3</item>
        <item>1</item>
    </string-array>
3. Then save the file and open the app. You should now see 'ApplesToPies' as an option for conversion.

Optional (advanced): You can also add a base number to apply during multiplication, this base number will be added before and subtracted after multiplication. To use this, simply add another string array named "units<profile name>Base" and add a single item in this array containing the value of the base number. Check the profile for "Temperature" for an example of how this works.
