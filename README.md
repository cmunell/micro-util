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

 