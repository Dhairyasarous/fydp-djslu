# Setting up the Bluetooth Library
1. Add the Bluetooth Library to the project: `File > New > Import Module > <path to bluetooth library>`
2. Add the library dependence to the project module: `File > Project Structure > <project module> > Dependencies > From Module > Select bluetoothdeviceselector`

# Connecting the modules initially
1. Start the BluetoothSelectorActivity
2. Connect the modules
3. Upon exiting this BluetoothSelectorActivity, the connected modules will automatically reconnect next time the app is started.

# Getting Bluetooth objects from the Bluetooth Library
The following code can be placed in the `onCreate` of any Activity.

    BluetoothContext bluetoothContext = (BluetoothContext) getApplicationContext(); 
    BluetoothHolder bluetoothHolder = bluetoothContext.getBluetoothHolder();
    Bluetooth mBtBSD = bluetoothHolder.getBluetoothBSD();
    Bluetooth mBtRightNav = bluetoothHolder.getBluetoothRightNav();
    Bluetooth mBtLeftNav = bluetoothHolder.getBluetoothLeftNav();

# Communicating with Bluetooth devices using the Bluetooth Objects
## Send
1. Check if bluetooth is connected `bluetoothObj.isConnected()`.
2. Send message using `bluetoothObj.send("MESSGAGE");`.

## Receive
1. Implement `Bluetooth.CommunicationCallback` in the Activity.
2. Register the callback to the Bluetooth Object (i.e. `bluetoothObj.registerCommunicationCallback(this)`). 
3. The `onMessage` function will be notified when the message arrives.
4. `onMessage` also returns an ID which corresponds to either `BT_ID_BSD`, `BT_ID_LEFT_NAV` or `BT_ID_RIGHT_NAV` so you know which module the message was receieved on.
