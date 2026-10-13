import com.sun.tools.attach.VirtualMachine;

/** Attach the bounded shop diagnostic to the existing, already logged-in game. */
public final class HsoAttach {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("PID agent.jar workspace");
        VirtualMachine vm = VirtualMachine.attach(args[0]);
        try { vm.loadAgent(args[1], args[1] + "|" + args[2]); }
        finally { vm.detach(); }
        System.out.println("Shop diagnostic attached to the running game; see hso_remote_sell_probe.log");
    }
}
