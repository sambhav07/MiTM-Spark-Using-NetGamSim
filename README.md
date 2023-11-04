### Author : Sambhav Jain

### Email Id : sjain218@uic.edu

### Video Link: https://youtu.be/e6XiIUHZmpE

# MiTM-Spark-Using-NetGamSim

Welcome to `MiTM-Spark-Using-NetGamSim`! This project leverages the capabilities of Spark and integrates it with NetGamSim for efficient and scalable MiTM (Man-in-the-Middle) simulations. This project implements a graph processing tool using Apache Spark's GraphX to perform random walks on graphs. We performs random walks, calculates similarity metrics, and generates statistics related to the walks such as number of successful/failed attacks based on a similarity threshold.

<img src="https://drive.google.com/uc?export=view&id=1gRxmqcYrNuqALq7IkcY6Kk16TNwaoLCO" width="300" alt="Description"/>

# Steps and Workflow
### 1. Graph Generation
Utilized **NetGraphSim** to generate a `.ngs` file containing the original and perturbed graphs.

### 2. Parsing the .ngs File
- Parsed the `.ngs` file to load both the original and perturbed graphs.
- Extracted node objects and edges to create graph object using GraphX library.
- Extracted action objects from both graphs to create shards for edges.

### 3. Spark Context Initialization
- Initialized Apache Spark's context to enable distributed computing.
- Configured Spark with the application name "RandomWalkGraphX" and set the master to "local[*]" to use all available cores.

### 4. Data Loading and Preparation
- Loaded the original and perturbed node objects and their relationships from the URLs provided via the command-line arguments.
- Filtered nodes containing valuable data and combined node lists for further processing.

### 5. Graph Construction with GraphX
- Parallelized the collection of nodes and edges and constructed a GraphX `Graph` object.
- Utilized Spark RDD (Resilient Distributed Dataset) transformations to distribute graph data across the Spark cluster.

### 6. Random Walk Simulation
- Implemented a `randomWalkFromNode` function to simulate a random walk from a given start node.
- Utilized the `randomWalk` function to perform random walks from different starting nodes until the desired graph coverage is reached.
- Kept track of visited nodes and the paths taken in each random walk.

### 7. Similarity Assessment
- Created a similarity cache to store computed similarity scores between nodes, reducing redundant computations.
- Computed similarity scores for pairs of nodes during random walks to assess the likelihood of successful and failed attacks.

### 8. Attack Analysis
- Separated the results of the random walks into successful and failed attacks based on predefined similarity thresholds.
- Stored details of each attack for further analysis.

### 9. Output and Metrics Calculation
- Printed paths of random walks and computed various statistics such as the number of walks, path lengths, and success rates.
- Calculated and stored metrics for the optimal number of walks required to achieve desired coverage.

### 10. Results Writing
- Used the `writeStatsToFile` function to output the final statistics and details of attacks to a specified file.

![Description](https://drive.google.com/uc?export=view&id=1yqKVq_nQDrBiFquOWRCGPN-ZjSK5YTQ1)


## Usage

To use this project, you will need to have Apache Spark installed and configured. Compile the project with SBT or your preferred Scala build tool, and run the application with the required command-line arguments.

## Project Structure

Here's a brief overview of the top-level directories and files in this project:

```plaintext
.
├── src/                          # Source files.
│   ├── main/
│   │   └── scala/
│   │       ├── NetGraphAlgebraDefs/
│   │       │   └── NetGraphComponent.scala
│   │       ├── similarity/
│   │       │   └── SimilarityMetrics.scala
│   │       ├── utilities/
│   │       │   ├── GraphUtilities.scala
│   │       │   └── NetGraph.scala
│   │       └── Main.scala
│   └── test/
│       └── scala/
│           ├── similarity/
│           │   └── SimilarityMetricsTest.scala
│           └── utilities/
│               └── GraphUtilitiesTest.scala
├── target/                       # Compiled files.
├── build.sbt                     # SBT build configuration.
├── project/
│   ├── build.properties
│   └── plugins.sbt
├── .bsp/                         # BSP configuration files.
├── .idea/                        # IDEA configuration files.
├── inputs/                       # Input data for processing.
└── outputs/                      # Output data after processing.
```



## How to Run the Project

You can execute the project either locally or on AWS EMR.

### Locally

#### 1. Clone the GitHub Repository
`git clone https://github.com/sambhav07/MiTM-Spark-Using-NetGamSim.git`<br>
`cd MiTM-Spark-Using-NetGamSim/`

#### 2. Build the Project
`sbt clean compile/`<br>
This ensures that all the dependencies from build.sbt are resolved and indexed locally.

#### 3. Set Program Arguments
- Go to edit configurations and on the top left corner click on `+` button and which will prompt a dialogue box to add new configuration
- Select Application and then give the name to the configuration, provide the module name along with the main class which is your entry point for the execution of the project
- Pass the program arguments from edit configurations as below:<br>
`"/Users/sambhavjain/Desktop/newrepo/sample spark project/inputs/NetGameSimNetGraph_01-11-23-22-52-52.ngs"
"/Users/sambhavjain/Desktop/newrepo/sample spark project/inputs/NetGameSimNetGraph_01-11-23-22-52-52.ngs.perturbed"
"/Users/sambhavjain/Desktop/newrepo/sample spark project/inputs/NetGameSimNetGraph_01-11-23-22-52-52.ngs.yaml"
"/Users/sambhavjain/Desktop/newrepo/sample spark project/inputs/fixed_NetGameSimNetGraph_01-11-23-22-52-52.ngs.yaml"
"/Users/sambhavjain/Desktop/newrepo/sample spark project/outputs/attackStats.txt"`<br>
##### Note: The above files should be present in your local system and hence give the path accordingly

#### 4. Run the Main File
Execute Main to start the project. The final output files can be found at -:<br> `/Users/sambhavjain/Desktop/newrepo/sample spark project/outputs/attackStats.txt`.

### 5. Run the Tests
To ensure the functionality and correctness of the implemented logic, execute the test suites available in the project using command `sbt test` . Test results will be displayed in the terminal, showcasing passed and failed tests.

### AWS EMR
For Initial Setup
Create an AWS S3 bucket to host the JAR and necessary input files (i.e., .ngs and .yaml files).
Inside the bucket, create input, jar, and output folders.
Running Steps
#### 1. Build the JAR
`sbt clean compile`<br>
`sbt -mem 2048 assembly`

#### 2. Deploy the JAR to S3
Upload the JAR into the jar folder in the S3 bucket.

#### 3. Place Input Files on S3
Add the .ngs files for the original and perturbed graphs, along with the .yaml file.

#### 4. Invoke the EMR Cluster
Use the following arguments:

`https://mitmfiles.s3.amazonaws.com/input/NetGameSimNetGraph_01-11-23-22-52-52.ngs
https://mitmfiles.s3.amazonaws.com/input/NetGameSimNetGraph_01-11-23-22-52-52.ngs.perturbed
https://mitmfiles.s3.amazonaws.com/input/NetGameSimNetGraph_01-11-23-22-52-52.ngs.yaml
https://mitmfiles.s3.amazonaws.com/input/fixed_NetGameSimNetGraph_01-11-23-22-52-52.ngs.yaml
s3://mitmfiles/outputs/attackStats.txt`

#### 5. View the Results
After the jobs are completed, results can be viewed in the output directory in the S3 bucket.

## Output:
For a set of 301 nodes in the original and perturbed graph, the output is as follows:
<p float="left">
  <img src="https://drive.google.com/uc?export=view&id=1oweP1B8B4lYH0jcu6QtZOM5oqMseArzB" width="500" />
  <img src="https://drive.google.com/uc?export=view&id=1HyZpzSvOBYL0yU-Urpfr4VV2hCJjMQ62" width="500" /> 
  <img src="https://drive.google.com/uc?export=view&id=1YHSz6g-7a9LF25WiGloyx134QbBu-8YK" width="500" />
</p>

## Iterations Analysis:
![Description](https://drive.google.com/uc?export=view&id=1ABo3UkhZssF2QxevQmaSBGd_eFzwJbMs)

