from flask import Flask, request, Response, jsonify
from flask_cors import CORS
import pexpect
import threading
import uuid

app = Flask(__name__)
CORS(app)

# --- Global state for our persistent shell ---
shell_process = None
lock = threading.Lock()

# A simple, predictable prompt that will force the shell to use.
SIMPLE_PROMPT = "PROMPT>>> "

def get_clean_output(command_to_run):
    """
    This is the clean function. It sends the user's command and a unique
    marker on the same line. It reads all text until it sees the marker, which
    guarantees it has captured exactly the output of the command and nothing more.
    """
    global shell_process
    
    # Generate a unique string to mark the end of our output.
    marker = str(uuid.uuid4())
    
    # --- THIS IS THE KEY FIX ---
    # Send the user's command and the marker command on a single line.
    # The shell will execute them in sequence before printing a prompt.
    full_command_line = f"{command_to_run}; echo {marker}"
    shell_process.sendline(full_command_line)
    
    # Wait for the unique marker to appear in the output.
    shell_process.expect_exact(marker, timeout=10)
    
    # The 'before' buffer contains exactly the clean output of the user's command.
    output = shell_process.before.decode('utf-8', 'ignore').strip()
    
    # Buffer cleaned up by waiting for the main shell prompt to appear.
    shell_process.expect_exact(SIMPLE_PROMPT)
    
    return output

@app.route('/start', methods=['POST'])
def start_shell():
    """Starts and robustly configures a new persistent bash shell."""
    global shell_process
    with lock:
        if shell_process is None or not shell_process.isalive():
            try:
                # Start a clean shell.
                shell_process = pexpect.spawn('/bin/bash --noprofile --norc', timeout=10)
                shell_process.expect(r'[\$#]\s')
                
                # Use our new robust function to send setup commands.
                # This ensures the buffer is clean and prevents all lag.
                get_clean_output(f"PS1='{SIMPLE_PROMPT}'")
                get_clean_output('stty -echo')
                get_clean_output('bind "set enable-bracketed-paste off"')

                print("--- Shell started and configured successfully ---")
                return jsonify({"status": "started"})
            except Exception as e:
                print(f"--- AGENT FAILED ON STARTUP: {e} ---")
                return jsonify({"error": "Failed to start or configure shell."}), 500
        else:
            return jsonify({"status": "already running"})

@app.route('/stop', methods=['POST'])
def stop_shell():
    """Stops the persistent shell."""
    global shell_process
    with lock:
        if shell_process and shell_process.isalive():
            shell_process.terminate(force=True)
            shell_process = None
            return jsonify({"status": "stopped"})
        else:
            return jsonify({"status": "already stopped"})

@app.route('/send', methods=['POST'])
def send_command():
    """Sends a command to the running shell and returns its output."""
    global shell_process
    with lock:
        if not shell_process or not shell_process.isalive():
            return jsonify({"output": "Shell is not connected."})
        
        try:
            data = request.get_json()
            command = data.get('command', '')
            
            # Use our new robust function to get the clean output.
            output = get_clean_output(command)
            
            return jsonify({"output": output})
        except Exception as e:
            return jsonify({"output": f"\n--- Error during send: {e} ---"})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=9090)
