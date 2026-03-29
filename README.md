# Mume

A cross-platform spectral analysis, storage, and classification ecosystem. The system is designed around the concept of a "Mume"—a discrete unit of spectral information, analogous to a memetic unit. The overa;; aim is to emphasise a "human in the loop" style paradigm to ML and future developments in MI, visualisation and sonification in implied through the design language. Leveraging the beads library and a portable Java solution should leave the project open to rapid prototyping of musical performance systems, and likewise the MXJ Java and Max implementation should be useful in the context of notation generation i.e. using MaxScore, and allow for easy integration into max4live.

Mume currently exists in two primary implementations sharing a common core:
1. **Mume-Max**: A collection of Java-based Max/MSP (MXJ) objects.
2. **Mume-Standalone**: A standalone Java implementation using the Beads audio library.

**Note:** This package was initially developed for use in real-time performance with the `zsa.freqpeak` object but will work with any list of frequency/amplitude pairs.

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
