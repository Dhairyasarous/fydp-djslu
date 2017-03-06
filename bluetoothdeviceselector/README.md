# Setting up the Bluetooth Library
1. Add the Bluetooth Library to the project: `File > New > Import Module > <path to bluetooth library>`
2. Add the library dependence to the project module: `File > Project Structure > <project module> > Dependencies > From Module > Select bluetoothdeviceselector`

# Getting Bluetooth objects using the Bluetooth Library
1. Start the BluetoothSelectorActivity
2. Connect the modules
3. Press Back when all of the modules have been selected
4. The Bluetooth Objects can be accessed from the `onActivityResult` by:

    <pre><code>BluetoothContext bluetoothContext = (BluetoothContext) getApplicationContext(); 
    BluetoothHolder bluetoothHolder = bluetoothContext.getBluetoothHolder();
    Bluetooth mBtBSD = bluetoothHolder.getBluetoothBSD();
    Bluetooth mBtRightNav = bluetoothHolder.getBluetoothRightNav();
    Bluetooth mBtLeftNav = bluetoothHolder.getBluetoothLeftNav();</pre></code>

# Communicating with Bluetooth devices using the Bluetooth Objects
1. Send: `mBtBSD.send("MESSGAGE");`
2. Receive: create/implement `Bluetooth.CommunicationCallback` and register it using `mBtBSD.registerCommunicationCallback(callback)`. The `onMessage` function will be notified when the message arrives.
