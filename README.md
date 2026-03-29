# Mume

A cross-platform spectral analysis, storage, and classification ecosystem. The system is designed around the concept of a "Mume"—a discrete unit of spectral information, analogous to a memetic unit. 

Mume currently exists in two primary implementations sharing a common core:
1. **Mume-Max**: A collection of Java-based Max/MSP (MXJ) objects.
2. **Mume-Standalone**: A standalone Java implementation using the Beads audio library.

**Note:** This package is primarily intended for use with the `zsa.freqpeak` object but will work with any Max list of frequency/amplitude pairs.

## System Structure

### 1. mume-core
The shared logic and data structures used by all implementations.
- **MumeData.java**: Defines the shared "Full State" list format used for communication and storage.
- **FrequencyBin.java**: Shared histogramming logic for spectral mass calculation.

### 2. mume-max
The original MXJ implementation for the Max/MSP environment.
- **Mume.java**: The primary analysis engine. Builds statistical histograms from real-time spectral data.
- **MumePlex.java**: A library and registry for storing and recalling Mume states.
- **MumeClassify.java**: A Nearest-Neighbor classifier for spectral profiles.

### 3. mume-standalone
A standalone implementation using the **Beads** audio library.
- **MumeBeads.java**: Main application entry point for resynthesis.
- **MumeAnalyzer.java**: Real-time spectral analysis engine for Beads.
- **MumeUI.java**: Graphical interface for "Human-in-the-Loop" spectral curation.

## Data Format
The entire ecosystem communicates using a standardized "Full State" list:
`[index, ID, top_freq, top_amp, total_weight, threshold, freq1, amp1, freq2, amp2, ...]`
