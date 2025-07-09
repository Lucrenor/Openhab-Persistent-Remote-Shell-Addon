# remoteshell Binding

_This binding allows you to connect to your local Linux machines over HTTP, providing a persistent shell session to manage them remotely from within OpenHAB._

_It is designed to give you stateful control, meaning you can run a series of commands like `cd` to change directories, and the session will remember its state for the next command._

_This is achieved by pairing the binding with a lightweight Python agent running on the target machine._

## Supported Things

_This binding supports a single Thing type for connecting to a remote Linux machine._

- `remoteDevice` - Represents a single remote Linux machine accessible over the network.


## Discovery

_This binding does not support any auto-discovery features. Each remote device must be configured manually as a Thing in OpenHAB._

## Binding Configuration

_This binding does not require any global configuration in the `services/remoteshell.cfg` file. All configuration is done at the Thing level._

## Thing Configuration

_To use the binding, you must manually create a `Remote Linux Device` Thing for each machine you want to control. This can be done through the OpenHAB UI or a `.things` file._

_The following configuration parameters are available:._

### `sample` Thing Configuration

| Name            | Type    | Description                                                                  | Default | Required | Advanced |
|-----------------|---------|------------------------------------------------------------------------------|---------|----------|----------|
| hostname        | text    | The hostname or IP address of the target device where the agent is running.  | N/A     | yes      | no       |
| port            | integer | The port that the Python agent is listening on.                              | 9090    | yes      | no       |

## Channels

_The `remoteDevice` Thing has the following channels:_

| Channel      | Type   | Read/Write  | Description                                                                                            |
|--------------|--------|-------------|--------------------------------------------------------------------------------------------------------|
| shellControl | Switch | R/W         | Toggles the persistent shell session on the remote agent. Turn it ON to connect and OFF to disconnect. |
| command      | String | W           | Sends a shell command to the active session on the remote agent.                                       |
| lastOutput   | String | R           | Displays the plain text output from the last executed command.                                         |

## Full Example

_Here is a full example of how to configure and use the binding with textual configuration files (`.things`, `.items`, and `.sitemap`)._

### Thing Configuration

```remoteshell.things
// This file defines a Thing for a remote machine with the IP 192.168.1.106
binding.remoteshell:remoteDevice:kali "Kali Linux Shell" [ hostname="192.168.1.106", port=9090 ]
```

### Item Configuration

```remoteshell.items
// Items to link to the channels of our Kali Linux Shell Thing
Switch Kali_Shell_Connection "Shell Connection" { channel="remoteshell:remoteDevice:kali:shellControl" }
String Kali_Shell_Command   "Command Input"    { channel="remoteshell:remoteDevice:kali:command" }
String Kali_Shell_Output    "Last Output"      { channel="remoteshell:remoteDevice:kali:lastOutput" }
```

### Sitemap Configuration

```remoteshell.sitemap
sitemap remoteshell label="Remote Shell Control" {
    Frame label="Kali Linux Terminal" {
        Switch item=Kali_Shell_Connection
        Webview url="http://192.168.1.106:9090/get_last_output" height=15 // A simple way to see output
    }
    Frame label="Command Input" {
        Setpoint item=Kali_Shell_Command label="Command"
    }
}
```
_Note: The `Webview` in the sitemap is a simple method for basic UIs. For the best experience, it is recommended to use the YAML code provided in the `ui-code.yaml` file on a Main UI page._

## Architecture & Agent Setup

_This binding requires a lightweight Python agent to be running on each target machine._

### Architecture

- The Python Agent (`agent.py`): A Flask server that runs on the target Linux machine. It uses `pexpect` to spawn and manage a persistent bash shell. It listens for commands from the OpenHAB binding.
- The OpenHAB Binding (`.jar` file): The addon that runs on your OpenHAB server. It communicates with the Python agent to start/stop the shell and send commands.

### Python Agent Setup
_This agent must be running on every Linux machine you wish to control._
#### Prerequisites:
- Python 3.x
- pip
#### Setup Steps:
- Download the `agent.py` file from this repository to a directory on your target machine.
- Open a terminal, navigate to that directory, and create a Python virtual environment:
```bash
python3 -m venv venv
source venv/bin/activate
```
- Install the required libraries inside the active environment:
```bash
pip install pexpect flask flask-cors
```
- Start the agent:
```bash
python3 agent.py
```
_The agent will now be listening for requests on port `9090.`_




