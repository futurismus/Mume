# mxj Mume

A collection of Java-based Max/MSP (MXJ) objects for spectral analysis, storage, and classification. The system is designed around the concept of a "Mume"—a discrete unit of spectral information, analogous to a memetic unit.

**Note:** This package is primarily intended for use with the `zsa.freqpeak` object but will work with any Max list of frequency/amplitude pairs.

## Files and Components

### 1. Mume.java
The primary analysis engine. It ingests real-time spectral data (frequency/amplitude pairs) and builds a statistical histogram over time.
- **Histogramming:** Groups frequencies into "bins" based on a user-defined resolution.
- **Weighting:** Calculates the "Spectral Mass" or energy of specific frequencies.
- **State Management:** Manages metadata including a unique ID and an activation threshold that determines when the Mume is "significant" enough to be output or stored.

### 2. MumePlex.java
A library and registry for Mume data. It serves as a central database for storing the "Full State" of various Mumes.
- **Indexing:** Allows retrieval of spectral data by string ID or integer index.
- **Data Persistence:** Holds collections of spectral vectors that can be recalled or sequenced ("dumped").
- **I/O:** Interfaces with `ioscbank~` by outputting formatted spectral vectors.

### 3. MumeClassify.java
A Nearest-Neighbor classifier for spectral profiles.
- **Training:** Learns from the output of `Mume` or `MumePlex` to build a memory of known spectral states.
- **Classification:** Compares live spectral input against its memory using a frequency-normalized Euclidean distance.
- **Visualization:** Outputs a "distance landscape," showing how closely the live input matches every stored model in the library.

### 4. MumeData.java
A utility class that defines the shared data structure used by all other objects. 
- **Standard Format:** Ensures that the "Full State" list—containing the index, ID, frequency, amplitude, weight, threshold, and spectral vector—is parsed and serialized consistently across the entire system.

## Data Format
The system communicates using a standardized "Full State" list:
`[index, ID, top_freq, top_amp, total_weight, threshold, freq1, amp1, freq2, amp2, ...]`
