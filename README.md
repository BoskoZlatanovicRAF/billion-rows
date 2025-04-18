# Meteorological Data Processing System

## Project Overview
This project implements a concurrent multi-threaded system for processing large meteorological data files (`.txt` and `.csv`) containing weather station names and temperature readings. The system monitors a specified directory for changes, processes the files, and provides a command-line interface for users to execute various queries and tasks on the data.

## Key Features

### Directory Monitoring
- Watches a specified directory for new or modified `.txt` and `.csv` files
- Detects changes using file timestamps to avoid redundant processing
- Processes files in chunks to handle extremely large files (up to 14GB)

### Concurrent Data Processing
- Uses an ExecutorService with multiple threads to process files in parallel
- Maintains an in-memory map of meteorological stations organized alphabetically
- Safely handles concurrent read/write operations with proper synchronization

### Command-Line Interface
- Supports flexible command input with both long (`--argument`) and short (`-a`) argument formats
- Commands can be entered in any order with proper validation
- Non-blocking design that doesn't interfere with background processing

### Supported Commands
- `SCAN`: Search files for stations with names starting with a specific letter and temperatures within a range
- `STATUS`: Check the status of a specific job (pending, running, completed)
- `MAP`: Display the in-memory map's current state
- `EXPORTMAP`: Export the in-memory map to a CSV file
- `SHUTDOWN`: Gracefully shut down the system with an option to save pending jobs
- `START`: Start the system with an option to reload previously saved jobs

### Additional Features
- Periodic automatic reporting of system state (every minute) to a log file
- Proper job management with unique job identifiers
- Thread-safe file operations using read/write locks
- Graceful error handling and system shutdown

## Technologies Used

- **Java**: Core programming language
- **Concurrency Tools**:
  - Thread management
  - ExecutorService for thread pooling
  - ReentrantReadWriteLock for synchronization
  - BlockingQueue for thread-safe communication
- **File I/O**:
  - BufferedReader/BufferedWriter for efficient file operations
  - Stream processing for handling large files without loading them entirely into memory
- **Data Structures**:
  - ConcurrentHashMap for thread-safe collections
  - TreeMap for sorted collections

## System Architecture

The system is composed of several interacting components:
- **DirectoryMonitor**: Watches for file changes
- **FileUpdateProcessor**: Processes updated files and updates the in-memory map
- **CommandDispatcher**: Manages user commands and delegates tasks
- **JobManager**: Handles job execution and status tracking
- **PeriodicReporter**: Generates automatic system reports
- **CLIThread**: Manages user input without blocking


## How to run?
- To run add some txt or csv file with format `Hamburg;12.0` with each station in new line. Here you can try some commands:
`SCAN --min 10.0 --max 20.0 --letter H --output output.txt --job job1`<br/>
`STATUS --job job1`<br/>
`MAP`<br/>
`SHUTDOWN --save-jobs`<br/>
`START --load-jobs`<br/>
