
BlaBoO
=======


Install BlaBoO
--------------
0. Install [JVM 8+](https://java.com/en/download/) and [maven](https://maven.apache.org/download.cgi)

1. Download the source and navigate in the home directory (BlackBoxOptimizer)

2. execute: `$mvn package` 

3. in the `target` folder you will find an `App` folder that contains the necessary folder structure with the single runnable `jar` file. You can compy this folder any place you want.


Run the software with GUI
--------------

- Start the server (this might require administrator rights):
    
    1. simple double click on the jar 
                    
    2. from command line in the `App` folder execute `sudo java -jar BlaBoO-1.0-SNAPSHOT-shaded.jar`

- go to `http://localhost:4567/hello` to access the GUI

- the first thig you see here is a simple setup for an optimization task.

GUI menu
-

##Start new BBO task

- the first thig you see here is a simple setup for an optimization task, whose setup file is stored in the examples folder.

- when you want to start a task from scratch you can chose `Start task` menu

##Load already existing task


- you can browse and load in already done configuration using `Load task` menu 

- here can you load backups from former experiments and resume the work

##Results


- the results of the optimization tasks will be storesdin the `results` folder, while the setups of the task are saved in the `experiments` folder

- When the computations have been finished, the values of the objectives in function of the parameter setup will be stored in the 'results' folder in a csv file of a name constructed from the name of the optimizer algorithm and the time the computation have been started at.

- in the browser as soon as the work finished the results will appear too in a chart, that displays the the objectives of the last couple computations. In this chart thediagrams visualize the results stored in the csv files located at in the results folder recently. To get rid of non interesting results, you should remove the unineresting files. if you want the visualization of one single algorithm you can click on the description in the legend, what temporally removes the other charts.

- to display the recent results, if the server is running you can use the `http://localhost:4567/results` url as well.


Setup an experiment
-

##Command


- specify the terminal command using '$<paramname>' at the params to be optimized

	- example: 'python Rosenbrock1.py $firstParam $secondParam'

##Params

- the params annotated with '$' must be deefined in the 'Params' section.

	- to add a **numeric** new param :

		- push 'Add param'

		- specify its name and type

		- the default value	and its boundaries

	- to add an **emumerated** param

		- choose type `Enum`

		- specify the possible values separated by ';'

		- setup the default value and the first and last options as boundaries

	- to add an **boolean** param

		- choose type 'Boolean'

		- check the checkboy for set default value to true

	- to add an function param

		- choose type 'Function'

		- specify the formule generating the series of values (in js style). For now the system handles one variable in the formula, that should be marked with '$'. That means that any string starting with an '$' will be replaced by a natural value at evaluation ('$foo' and '$bar' will. be handled as one variable). Formula type now generates a series of numbers computed from '[0,1,2,..]' using the formula.

			example: 	'1/$alma' will generate a series of '[INFINITY ,1 ,1/2 ,1/3 , ... ]'

		- the next input defines how long the generated series should be.

##Params depending on other params (beta)


- the ranges the parameters move in can depend on the value of other parameters, or it is possible that setting a parameter only makes sense if an other parameter has in a specified range. (think on SVM parametrization in machine learning where some parameters have meaning only in case of uding given kernels )

- to add such dependency to a parameter you can click on the 'New param dependency' button, then you should choose the variable from the select on which our parameter depends, then specify in case of what values will bw our parameter in the range we have given before.

- If there are more possible range/value that our parameter can take, we can add new ranges to the parameter for the different cases, If for one of the ranges we don't specify any dependencies, we can regard as that will be the default behaviour of the parameter if none of the other dependencies comlies.

##Objectives

- to evaluate the quality of the parameter setup the software needs to know value of the objective function. Now we expect the blackbox function to write this value(s) on standard output, or in a specified file from where we can read it. The format of this file is: '<name of objective> <value>'.

- here we need to specify the file contains the values

- the maximum number of iterations we want to allow to find the best possible setup

- and the characteristics of the objectives to be given similarly to the 'Param' descriptions. Here we can specify the type of the objective, that can beto be minimized, maximized, less or greater then or equal to a specific value. We allow to use a linear combination of multiple objectives, in this case we can specify the importance of the target function at the 'Weight' parameter.

- if the algorithm does not submit the expected objective measurements, the value will be set to `0`/`false`.

##Safe mode and restart tasks

- if check the 'Safe mode' option at the bottom of the page the software will save the state of the optimization at a frequency given in the next input(per iterations)

- if we want to repeat a task or just continue an interrupted one, at the top of the page we can browse a backup or a setup file, in which the setup or the last state of the interrupted optimization task is stored. These files should be in the 'backup' or the 'experiments' folder inside the software's working directory.

#The optimizer algorithms

- on the next page we can chose what algorithm we want to use to optimize the parameters. After choosing the algorithms we can set the parameters of the optimizer, that will be executed after pushing the 'clickme' button.


Command Line Use
-

You can run tuning tasks already specified in a `json` file. To do that, navigate to the App directory and execute:
`java -jar BlaBoO-1.0-SNAPSHOT-shaded.jar -r examples/commandline/SVM_python_GridSearch_cl.json `
Here `-r` indicates that we want to run a standalone  optimization task, without that the server application will be launched, thus you can access the browsert GUI and modify the setups if you want to.

You can acces save mode from command line as well using the following flags:

- use `-s` for default backups, then at every 10.  iteration the state of the optimization will be stored in a backup file.

- use `-sp <frequency>` for save state at every <frequency> iteration the state will be saved

Recover interrupted process
-

You can restart an interrupted process by loading the backup files that have been stored in `App/backup` folder. That is either xou browse it from gui as setup file, or give as argument in command line mode.


User support
-

I you have any question, remark or suggestion regarding the project, please contact us at: axx6v4(at)inf.elte.hu. We are happy to get any feedback, or contribution.






		


