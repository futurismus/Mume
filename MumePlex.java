import com.cycling74.max.*;
import java.util.*;

/**
 * MumePlex: A storage and retrieval system for Mumetic spectral data.
 * Receives full-state lists from the Mume object and stores them indexed by ID and index.
 * 
 * Max Messages:
 *  - list [index, ID, freq, amp, weight, thresh, spectral...]: Store data from a Mume object
 *  - lookup [ID]: Recall a full state by string ID out Outlet 0
 *  - get [index]: Recall a full state by integer index out Outlet 0
 *  - getvector [ID/index]: Output only the spectral components out Outlet 0
 *  - getkeys: Output a summary of all IDs, indices, and weights out Outlet 1
 *  - dump: Sequence all stored states through Outlet 0
 *  - clear: Empty the library and index map
 *  - size: Output current library size out Outlet 1
 */
public class MumePlex extends MaxObject {
    // Storage: Key is mumeID, Value is MumeData object
    private Map<String, MumeData> library = new LinkedHashMap<>();
    // Secondary index to allow lookups by integer mePoint
    private Map<Integer, String> indexMap = new HashMap<>();

    public MumePlex(Atom[] args) {
        declareInlets(new int[]{DataTypes.ALL});
        declareOutlets(new int[]{DataTypes.ALL, DataTypes.ALL}); // Outlet 0: Data, Outlet 1: Notifications
    }

    /**
     * Receives a list from a Mume object.
     * Expected format: [index, ID, freq, amp, weight, thresh, spectral_data...]
     */
    public void list(Atom[] list) {
        if (list.length >= 6) {
            MumeData data = new MumeData(list);
            
            // Store in the library
            library.put(data.id, data);
            indexMap.put(data.index, data.id);

            post("MumePlex: Stored Mume '" + data.id + "' at index " + data.index + " (Vector size: " + data.spectralData.length + ")");
            updateCount();
        } else {
            error("MumePlex: Received invalid Mume data. List too short.");
        }
    }

    /**
     * Retrieve a Mume by its string ID.
     */
    public void lookup(Atom[] args) {
        if (args.length == 0) return;
        
        String id = args[0].getString();
        boolean vectorOnly = (args.length > 1 && args[1].getString().equals("vector"));

        if (library.containsKey(id)) {
            outputMume(id, vectorOnly);
        } else {
            error("MumePlex: ID '" + id + "' not found.");
        }
    }

    /**
     * Retrieve a Mume by its integer index (mePoint).
     */
    public void get(Atom[] args) {
        if (args.length == 0) return;
        int index = args[0].getInt();
        boolean vectorOnly = (args.length > 1 && args[1].getString().equals("vector"));

        if (indexMap.containsKey(index)) {
            outputMume(indexMap.get(index), vectorOnly);
        } else {
            error("MumePlex: Index " + index + " not found.");
        }
    }

    private void outputMume(String id, boolean vectorOnly) {
        MumeData data = library.get(id);
        if (data == null) return;

        if (vectorOnly) {
            outlet(0, data.spectralData);
        } else {
            outlet(0, data.toFullState());
        }
    }

    /**
     * Output all stored Mume IDs along with their index and weight.
     * Format: [keys, id1, index1, weight1, id2, index2, weight2...]
     */
    public void getkeys() {
        String[] keys = library.keySet().toArray(new String[0]);
        Atom[] output = new Atom[(keys.length * 3) + 1];
        output[0] = Atom.newAtom("keys");
        
        for (int i = 0; i < keys.length; i++) {
            MumeData data = library.get(keys[i]);
            int base = (i * 3) + 1;
            output[base] = Atom.newAtom(data.id);
            output[base + 1] = Atom.newAtom(data.index);
            output[base + 2] = Atom.newAtom(data.weight);
        }
        outlet(1, output);
    }

    /**
     * Clear the library.
     */
    public void clear() {
        library.clear();
        indexMap.clear();
        post("MumePlex: Library cleared.");
        updateCount();
    }

    /**
     * Output the entire library sequentially.
     */
    public void dump() {
        for (MumeData data : library.values()) {
            outlet(0, data.toFullState());
        }
    }

    public void recall(Atom[] args) {
        lookup(args);
    }

    public void size() {
        updateCount();
    }

    private void updateCount() {
        outlet(1, new Atom[]{Atom.newAtom("size"), Atom.newAtom(library.size())});
    }

    public void anything(String msg, Atom[] args) {
        if (msg.equals("recall")) {
            recall(args);
        } else if (msg.equals("getvector")) {
            if (args.length > 0) {
                if (args[0].isString()) {
                    lookup(new Atom[]{args[0], Atom.newAtom("vector")});
                } else if (args[0].isInt()) {
                    get(new Atom[]{args[0], Atom.newAtom("vector")});
                }
            }
        } else if (msg.equals("dump")) {
            dump();
        } else if (msg.equals("clear")) {
            clear();
        } else if (msg.equals("getkeys")) {
            getkeys();
        } else if (msg.equals("size")) {
            size();
        }
    }
}
