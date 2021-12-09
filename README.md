# SInC: **S**emantic **In**ductive **C**ompressor
SInC is a semantic inductive compressor on relational data. It splits DBs into two parts where one can be semantically inferred by the other, thus the inferable part is reduced for compression. The compression is realized by iteratively mining for SQL queries until there are no more records that can be effectively covered by any other pattern.

## 1. Prerequisites

SInC is implemented in Java and require version 11+. All of its dependencies are publicly accessible via Maven.

Input data format is `.tsv` files. Each line of the file is the following format:

```
<relation>\t<arg1>\t<arg2>\t...<argn>
```

For example:

```
President	USA	Trump
Family	ginny	harry	albus
```

## 2. Usage

The basic class for compression is the abstract class `SInC`. To compress relational data, simply create a SInC implementation object and invoke `run()` on it. For exmaple:

```java
SincConfig config = new SincConfig(1,false,false,5,true,eval_type,0.05,0.25,false,-1.0,false,false);
SInC sinc = new SincWithRecalculateCache(config,"elti.tsv",null,null);
sinc.run();
List<Rule> hypothesis = sinc.getHypothesis();  // H
Set<Predicate> necessary = sinc.getStartSet();  // N
Set<Predicate> counter_examples = sinc.getCounterExamples();  // C
Set<String> delta_constants = sinc.getSupplementaryConstants();  // ∆Σ
```

Arguments in `SincConfig` are for debugging, experiments and possible extensions. Currently, the following arguments will take effect:

- `beamWidth`
- `evalMetric`
- `minFactCoverage`
- `minConstantCoverage`

Current implementation of `SInC` is `SincWithTabuPruning`, other implementations are for experiments. Arguments to initialize a SInC object include:

- `config`: A `SincConfig` object
- `kbPath`: The path to the input data file
- `dumpPath`: The path to compressed data file. If null, output will go to the terminal
- `logPath`: The path to a log file. If null, output will go to the terminal
