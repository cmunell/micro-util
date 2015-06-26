# micro-util

This repository contains a generic Java ML/NLP library used 
primarily with the NELL micro-reading projects .  The library helps 
with two main tasks:  

1.  Running text documents through NLP pipelines 
(e.g. the Stanford CoreNLP pipeline 
(http://nlp.stanford.edu/software/corenlp.shtml) and the
NELL micro-reading pipeline 
(http://rtw.ml.cmu.edu/wiki/index.php/Micro-reading)) 
to produce annotated documents in the NELL micro-reading
format (http://rtw.ml.cmu.edu/wiki/index.php/Annotation). 

2. Training/evaluating new models using featurized 
data sets constructed from the NLP annotated documents given
by 1.

For the purpose of 2, micro-util offers a 'ctx' scripting
language for defining features, models, and their evaluations.

## Setup and build ##

You can get the source code for the project by running

    git clone https://github.com/cmunell/micro-util.git
    
and then build by running

    mvn compile 
    
from the root directory of the project assuming that you have
Maven setup and configured to access the internal RTW repository
as described at http://rtw.ml.cmu.edu/wiki/index.php?title=Maven.

## Layout of the project ##

The code is organized into the following packages in the *src* directory 
(labeled with (1) and (2) to indicate which of the two main tasks mentioned
above the package is associated with):

*	*edu.cmu.ml.rtw.generic.cluster* (2) - for mapping strings,
token spans, and other objects into clusters.

*	*edu.cmu.ml.rtw.generic.data* (1),(2) - for cleaning data, 
representing dictionaries, and holding other miscellaneous data/functions
that can be used by models, features, annotators, etc.

*	*edu.cmu.ml.rtw.generic.data.annotation* (1),(2) - for 
holding data sets and annotated documents.

*	*edu.cmu.ml.rtw.generic.data.annotation.nlp* (1) - for
holding NLP documents and their annotations.

*	*edu.cmu.ml.rtw.generic.data.annotation.nlp.micro* (1) - taken 
from the main RTW repository for manipulating the NELL micro-reading
annotation format (http://rtw.ml.cmu.edu/wiki/index.php/Annotation)

*	*edu.cmu.ml.rtw.generic.data.annotation.structure* (2) - for organizing 
data points (datums) into structures used by structure prediction models.

*	*edu.cmu.ml.rtw.generic.data.feature* (2) - for featurizing data
sets so that they can be used by models.

*	*edu.cmu.ml.rtw.generic.data.feature.fn* (2) - for 
constructing compositions
of simple functions that are mainly used by 
*edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnDataVocab* to construct
arbitrarily complicated feature 
vocabularies in a systematic way.  This function
library was mainly created to support rule-based construction of 
new features in a feature grammar model as described in
*src/main/resource/docs/feature-grammar/FeatureGrammarNotes.pdf* and 
implemented in 
*edu.cmu.ml.rtw.generic.model.SupervisedModelLogistmarGrammression*.

*	*edu.cmu.ml.rtw.generic.data.feature.rule* (2) - for rules that 
transform features, functions, and models into new features, functions, and 
models.  This was mainly created to support rule-based construction of 
new features in a feature grammar model as described in
*src/main/resource/docs/feature-grammar/FeatureGrammarNotes.pdf* and 
implemented in 
*edu.cmu.ml.rtw.generic.model.SupervisedModelLogistmarGrammression*.

*	*edu.cmu.ml.rtw.generic.model* (2) - for various machine learning
models.

*	*edu.cmu.ml.rtw.generic.model.annotator* (1) - for generic 
representations of document annotators and annotator pipelines.

*	*edu.cmu.ml.rtw.generic.model.annotator.nlp* (1) - for representations
of NLP document annotators and NLP pipelines.

*	*edu.cmu.ml.rtw.generic.model.annotator.nlp.stanford* (1) - for
Stanford CoreNLP pipeline 
(http://nlp.stanford.edu/software/corenlp.shtml) custom annotator 
implementations.

*	*edu.cmu.ml.rtw.generic.model.constraint* (2) - for representing 
constraints on data sets.  These were only used by 
*edu.cmu.ml.rtw.generic.model.SupervisedModelPartition* to determine which
parts of the data should be used to train which models, but this currently
needs to be refactored.

*	*edu.cmu.ml.rtw.generic.model.evaluation* (2) - for carrying out
various evaluation procedures (grid search, cross-validation, etc)

*	*edu.cmu.ml.rtw.generic.model.evaluation.metric* (2) - for 
various evaluation measures (accuracy, F1, etc)

*	*edu.cmu.ml.rtw.generic.parse* (2) - for parsing the ctx scripting 
language in which models, features, and evaluation procedures are
specified.

*	*edu.cmu.ml.rtw.generic.util* (1),(2) - for configuring projects,
 running external commands, dealing with files, dealing with Hadoop, etc.

## Creating a project that uses micro-util ##

Assuming you have Maven installed and configured according to 
instructions at http://rtw.ml.cmu.edu/wiki/index.php?title=Maven,
you can have your project depend on micro-util by adding the following
to the dependencies element of your projects pom.xml:

    <dependency>
      <groupId>edu.cmu.ml.rtw</groupId>
      <artifactId>micro-util</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>

Assume in the following instructions that you're creating a project
called 'micro-proj' that uses micro-util. Regardless of whether 
micro-proj uses micro-util for (1) annotating documents
or (2) training/evaluating models, it will probably need 
to define some project specific infrastructure classes that extend 
some of the infrastructure classes from micro-util. Namely,
you'll want to create the following:

* *ProjDataTools* extending *edu.cmu.ml.rtw.generic.data.DataTools*. 
This is the class where you'll add any project specific gazetteers, string
cleaning fucntions, annotation types, or other objects containing general 
methods for manipulating data.  Classes like *ProjDataTools* that extend
*DataTools* act as a project specific global tool box that most other objects 
(e.g. models, features, evaluations, etc) in the system have access to.
Each of the tools in this global tool box has a string name that other
objects can refer to it by.  The main purpose of this tool box is that it
allows the the ctx scripting language and other configuration files to
specify that features, models, etc should be constructed to use certain tools
(e.g. gazetteers, string cleaning functions) that have been added to the 
system.  Also, the *ProjDataTools* class keeps a list of project-specific
annotation types that define annotation serialization/deserialization 
procedures used by *edu.cmu.ml.rtw.generic.data.annotation.Document* 
objects.  See *edu.cmu.ml.rtw.micro.cat.data.CatDataTools* in 
https://github.com/cmunell/micro-cat for an example of how to extend the
*DataTools* class.

* *ProjProperties* extending *edu.cmu.ml.rtw.generic.util.Properties*.
The*edu.cmu.ml.rtw.generic.util.Properties* is an extended version of the 
Java Properties class that allows for some extra functionality like having
system environment variables in property settings.  Projects that 
use micro-util usually have a class that extends 
*edu.cmu.ml.rtw.generic.util.Properties*, and implements hard-coded methods
for getting each project-specific property.  The motivation for the hard-coded
methods is that it allows your IDE to auto-complete the available properties 
when you're writing the code that access them, and it avoids distributing 
hard-coded property name strings throughout your code outside your 
*ProjProperties* class.  The configuration that 
is read by your *ProjProperties*
class should be in a file like *proj.properties* in your top-level directory
or in *src/main/resources*. 
 See *edu.cmu.ml.rtw.micro.cat.util.CatProperties*
and *src/main/resources/cat.properties* 
in https://github.com/cmunell/micro-cat
for examples.

To keep things organized, you might want 
micro-proj's package structure to mirror
micro-util.  Mainly, you would want to have a package like
*edu.cmu.ml.rtw.micro.proj.data* in 
which to place your *ProjDataTools* class 
that extends *edu.cmu.ml.rtw.generic.data.DataTools*, and a 
package like *edu.cmu.ml.rtw.micro.proj.util* in which to place your
*ProjProperties* class that extends 
*edu.cmu.ml.rtw.generic.util.Properties*.  
Similarly, depending on how you're
using micro-util, you might want to have a package like
*edu.cmu.ml.rtw.micro.proj.data.annotation.nlp* that 
contains project-specific
NLP annotation objects or a package like 
*edu.cmu.ml.rtw.micro.proj.data.feature* that 
contains project-specific 
feature implementations that extend 
*edu.cmu.ml.rtw.generic.data.feature.Feature*.  See 
https://github.com/cmunell/micro-cat for 
an example of package structure
that mirrors micro-util's package structure.

### Running documents through annotation pipelines ###

You can use annotation pipelines to annotate documents 
using code like the following (which annotates some
text using the Stanford CoreNLP pipeline):

    PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
    ProjDataTools dataTools = new ProjDataTools();
    DocumentNLP document = new DocumentNLPInMemory(dataTools, 
     "This is the name of the document.", 
     "This is some text to annotate.",
     Language.English, pipeline);
    
The *DocumentNLP* object will now contain the Stanford
pipeline's annotations, and it contains several methods for
serializing these annotations and storing them to disk.  For 
example, *toMicroAnnotation* method in *DocumentNLP* will
convert the annotations to the NELL micro-reading 
annotation format.

### Building document annotation types, annotators, and annotator pipelines ###

You can create project-specific document annotation types as instances
of the *edu.cmu.ml.rtw.generic.data.annotation.AnnotationType* class.
If your annotation types provide information about text from natural
language text documents, then you'll actually want to create instances
of *edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP*.
Creating such types, and then registering them with the system in your
*edu.cmu.ml.rtw.micro.proj.data.ProjDataTools* class (see more information
above) will allow them to be automatically serialized, deserialized, and
manipulated through *edu.cmu.ml.rtw.generic.data.annotation.Document*
objects.

For examples of existing annotation type definitions, see the 
first few lines of 
*edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP*
where token, part-of-speech, dependency parse, and other NLP annotation
types are defined.  You can also look at 
*edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat* in 
https://github.com/cmunell/micro-cat for examples of micro-cat 
project-specific annotation type definitions. One particular example,
the co-reference resolution annotation type definition, is shown here:

    public static final AnnotationTypeNLP<TokenSpanCluster> COREF 
    = new AnnotationTypeNLP<TokenSpanCluster>("coref", 
    TokenSpanCluster.class, Target.TOKEN_SPAN);

The three arguments in this definition specify a string name of the 
annotation type, the class in which it is stored, and the sort of thing it 
annotates (token span, document, sentence, or token). You can also optionally 
specify a class for deserializing/serializing 
the annotation type as a fourth object, but this should 
be unnecessary as long as your annotation type is 
stored as one the following:

* A String, Double, Integer, Boolean, or Enum type.

* A class implementing the *edu.cmu.ml.rtw.generic.util.StringSerializable* 
interface and containing a constructor with either a DocumentNLP argument or
DocumentNLP and sentence-index arguments (see for example
*edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse*)

* A class implementing the *edu.cmu.ml.rtw.generic.util.JSONSerializable*
interface constructor with either a DocumentNLP argument or
DocumentNLP and sentence-index arguments (see for example
*edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpanCluster*)

Note that if the annotation objects have some structure, 
then it's preferable that they are stored in classes implementing 
either *StringSerializable* or *JSONSerializable* so that the 
annotations are machine-readable without requiring some kind of
other parsing code written elsewhere. 

You can create a custom NLP annotator that produces your 
custom NLP annotation type by implementing one of the
following interfaces: 

    edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorSentence (one document per annotation)
    edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorDocument (one sentence per annotation)
    edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorToken (one token per annotation)
    edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan (one token span per annotation)

For each of these interfaces, you have to implement a method that produces 
your annotations, along with the following methods:

    String getName(); (name of the annotator)
    AnnotationType<T> produces(); (annotation type produced by the annotator)
    AnnotationType<?>[] requires(); (array of annotation types required by the annotator)
    boolean measuresConfidence(); (indicates whether the annotator gives confidence scores)

Here is an example implementation of the *AnnotatorTokenSpan* interface:
 
    public class AnnotatorExample implements AnnotatorTokenSpan<String>() {
        public static final AnnotationTypeNLP<String> EXAMPLE_ANNOTATION_TYPE 
         = new AnnotationTypeNLP<String>("example_type", String.class, Target.TOKEN_SPAN);
        public String getName() { return "example"; }
        public AnnotationType<String> produces() { return EXAMPLE_ANNOTATION_TYPE; };
        public AnnotationType<?>[] requires() { return new AnnotationType<?>[] { AnnotationTypeNLP.TOKEN }; }
        public boolean measuresConfidence() { return true; }
        public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
          	 [...]
           	 document.getToken(i, j);
            [Manipulate tokens retrieved from the document object]
            return [annotations];
        }
    }

After you've implemented your annotator, you can use it within an
annotation pipeline to annotate documents.  For example, the
following code will append a custom annotator to the end of the
Stanford CoreNLP pipeline, and then annotate a document:

    PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
    PipelineNLPExtendable pipelineExtendable = new PipelineNLPExtendable();
    pipelineExtendable.extend(new AnnotatorExample());
    PipelineNLP pipeline = pipelineStanford.weld(pipelineExtendable);
    ProjDataTools dataTools = new ProjDataTools();
    DocumentNLP document = new DocumentNLPInMemory(dataTools, 
     "This is the name of the document.", 
     "This is some text to annotate.",
     Language.English, pipeline);

### Training and evaluating models ###

After you've set up your project with a class that
extends *edu.cmu.ml.rtw.generic.data.DataTools* as described
above, there are just a few steps to setting up a 
program that trains and evaluates supervised models on some 
classification task:

1. Create a class extending 
*edu.cmu.ml.rtw.generic.data.annotation.Datum* 
to represent your training examples.  This class will
hold all the information that each training example consists
of.  Within this class definition, there should
also be a class that extends 
*edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools*.
Your extension of *Datum.Tools* is mainly for holding
project-specific features, models, evaluations, and other
objects that will be used on data sets containing training
examples of your project-specific type.  This serves a
similar purpose to the *edu.cmu.ml.rtw.generic.data.DataTools*
class, except that *Datum.Tools* classes only hold things
specific to particular kinds of data defined by an extension
to *Datum*.  See 
*edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.TokenSpansDatum*
in https://github.com/cmunell/micro-cat 
for an example of how the *Datum* class should be extended.

2. Create a ctx script that declares the models, features,
and evaluations you would like to use.  Here is an example
script that can be used by 
*edu.cmu.ml.rtw.generic.model.evaluation.ValidationGSTBinary* to
perform a grid-search procedure over several binary classifiation
models whose outputs are combined to do 
multiclass classification:
```
    value randomSeed="1";
    value maxThreads="33";
    value trainOnDev="false";
    value errorExampleExtractor="FirstTokenSpan";
    array validLabels=("label1","label2","label3);
    evaluation accuracy=Accuracy();
    evaluation f1=F(mode="MACRO_WEIGHTED", filterLabel="true", Beta="1");
    evaluation prec=Precision(weighted="false", filterLabel="true");
    evaluation recall=Recall(weighted="false", filterLabel="true");
    feature fner=Ner(useTypes="true", tokenExtractor="AllTokenSpans");
    feature ftcnt=TokenCount(maxCount="5", tokenExtractor="AllTokenSpans");
    feature fform=StringForm(stringExtractor="FirstTokenSpan", minFeatureOccurrence="2");
    model lr=Areg(l1="0", l2="0", convergenceEpsilon=".00001", 
    maxTrainingExamples="520001", batchSize="100", evaluationIterations="200",
    maxEvaluationConstantIterations="500", weightedLabels="false", 
    computeTestEvaluations="false")
    {
        array validLabels=${validLabels};
    };
    gs g=GridSearch() {
        dimension l1=Dimension(name="l1",
            values=(.00000001,.0000001,.000001,.00001,.0001,.001,.01,.1,1,10), 
            trainingDimension="true");
     	dimension ct=Dimension(name="classificationThreshold", 
     	    values=(.5,.6,.7,.8,.9), 
     	    trainingDimension="false");
     	model model=${lr};
     	evaluation evaluation=${accuracy};
    };
```
You can find more examples of ctx scripts in
the *src/main/resources/contexts/* directory of 
https://github.com/cmunell/micro-cat.

3. Create a program that constructs a set of your data
examples (extending *Datum*) from a set of documents,
and then runs some kind of validation from 
*edu.cmu.ml.rtw.generic.model.evaluation* using your
ctx script to load in the desired models, features, etc.
See *edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinary*
for an example.  Basically, the program needs to load
in some document sets using 
*edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentSetNLP*,
construct some data sets from the documents using
*edu.cmu.ml.rtw.generic.data.annotation.DataSet*,
deserialize the ctx script through
*edu.cmu.ml.rtw.generic.data.Context*, and then
give the resulting *Context* and *DataSet* to some kind
of *edu.cmu.ml.rtw.generic.model.evaluation.Validation*.
	
## Features, models, and evaluations ##

The following is a summary of the features, models, and evaluation
metrics that currently available in micro-util.

### Features (in *edu.cmu.ml.rtw.generic.data.feature*) ###

* Conjunction
* ConstituencyPath


### Models (in *edu.cmu.ml.rtw.generic.model* ###

* Areg
* Creg
* LabelDistribution
* SVM
* SVMStructured (currently needs to be refactored)
* LogistmarGramression
* CompositeBinary

### Evaluations (in *edu.cmu.ml.rtw.generic.model.evaluation.metric* ###


