package sinc2.rule;

import sinc2.common.Argument;
import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.common.Predicate;
import sinc2.kb.NumerationMap;
import sinc2.kb.Record;
import sinc2.util.DisjointSet;
import sinc2.util.MultiSet;

import java.util.*;

/**
 * The class of the basic rule structure. The class defines the basic structure of a rule and the basic operations that
 * manipulates the structure.
 *
 * @since 1.0
 */
public abstract class Rule {
    /** The index of the head predicate */
    public static final int HEAD_PRED_IDX = 0;

    /** The beginning index of the body predicate */
    public static final int FIRST_BODY_PRED_IDX = HEAD_PRED_IDX + 1;

    /** The threshold of the coverage value for pruning */
    public static double MIN_FACT_COVERAGE = 0.0;

    /** The cache of all used fingerprints */
    protected final Set<Fingerprint> fingerprintCache;

    /** The fingerprint tabu set of all rules that are pruned due to insufficient coverage */
    protected final Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap;

    /** The structure of the rule */
    protected final List<Predicate> structure;

    /** The number of limited variables used in the rule */
    protected final List<Integer> limitedVarCnts;   // Limited vars use non-negative ids (the list indices)

    /** The fingerprint of the rule */
    protected Fingerprint fingerprint;

    /** The rule length */
    protected int length;

    /** The evaluation of the rule */
    protected Eval eval;

    /**
     * Parse a plain-text string into a rule structure.
     *
     * @return The rule structure is represented by a list of ParsedPred because there is no mapping information for the
     * numerations of the names.
     */
    public static List<ParsedPred> parseStructure(String ruleStr) {
        List<ParsedPred> structure = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        String functor = null;
        List<ParsedArg> arguments = new ArrayList<>();
        for (int char_idx = 0; char_idx < ruleStr.length(); char_idx++) {
            char c = ruleStr.charAt(char_idx);
            switch (c) {
                case '(':
                    /* Buffer as functor */
                    functor = builder.toString();
                    builder = new StringBuilder();
                    break;
                case ')':
                    /* Buffer as argument, finish a predicate */
                    arguments.add(parseArg(builder.toString()));
                    ParsedPred predicate = new ParsedPred(functor, arguments.toArray(new ParsedArg[0]));
                    structure.add(predicate);
                    builder = new StringBuilder();
                    functor = null;
                    arguments = new ArrayList<>();
                    break;
                case ',':
                    /* In Predicate: Buffer as argument; Out of predicate: nothing */
                    if (null != functor) {
                        arguments.add(parseArg(builder.toString()));
                        builder = new StringBuilder();
                    }
                    break;
                case ':':
                case '-':
                    /* Nothing */
                    break;
                default:
                    /* Append buffer */
                    builder.append(c);
            }
        }
        return structure;
    }

    /**
     * Parse a plain-text string into an argument.
     *
     * @return The argument is represent by an instance of ParsedArg because there is no mapping information for the
     * numerations of the names.
     */
    static ParsedArg parseArg(String str) {
        switch (str.charAt(0)) {
            case 'X':
                /* Parse LV */
                return ParsedArg.variable(Integer.parseInt(str.substring(1)));
            case '?':
                /* Parse UV */
                return null;
            default:
                /* Parse Constant */
                return ParsedArg.constant(str);
        }
    }

    /**
     * Initialize the most general rule of a certain target head relation.
     *
     * @param headFunctor The functor of the head predicate, i.e., the target relation.
     * @param arity The arity of the functor
     * @param fingerprintCache The cache of the used fingerprints
     * @param category2TabuSetMap The tabu set of pruned fingerprints
     */
    public Rule(
            int headFunctor, int arity, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap
    ) {
        this.fingerprintCache = fingerprintCache;
        this.category2TabuSetMap = category2TabuSetMap;
        structure = new ArrayList<>();
        limitedVarCnts = new ArrayList<>();
        length = 0;
        eval = null;

        structure.add(new Predicate(headFunctor, arity));
        fingerprint = new Fingerprint(structure);
        this.fingerprintCache.add(fingerprint);
    }

    /**
     * Construct a rule with a certain structure.
     *
     * @param structure The structure of the rule.
     * @param fingerprintCache The cache of the fingerprint
     */
    public Rule(
            List<Predicate> structure, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap
    ) {
        this.fingerprintCache = fingerprintCache;
        this.category2TabuSetMap = category2TabuSetMap;
        this.structure = new ArrayList<>(structure.size());
        limitedVarCnts = new ArrayList<>();
        eval = null;

        /* Calculate the length and find the limited variables */
        length = 0;
        Map<Integer, Integer> old_vid_2_new_vid_map = new HashMap<>();
        for (Predicate p: structure) {
            this.structure.add(new Predicate(p));
            for (int argument: p.args) {
                if (Argument.isVariable(argument)) {
                    int var_id = Argument.decode(argument);
                    old_vid_2_new_vid_map.computeIfAbsent(var_id, k -> {
                        limitedVarCnts.add(0);
                        return old_vid_2_new_vid_map.size();
                    });
                }
                this.length += Argument.isEmpty(argument) ? 0 : 1;
            }
        }
        this.length -= old_vid_2_new_vid_map.size();

        /* Normalize the variables */
        for (Predicate p: this.structure) {
            for (int arg_idx = 0; arg_idx < p.arity(); arg_idx++) {
                int argument = p.args[arg_idx];
                if (Argument.isVariable(argument)) {
                    int new_vid = old_vid_2_new_vid_map.get(Argument.decode(argument));
                    p.args[arg_idx] = Argument.variable(new_vid);
                    limitedVarCnts.set(new_vid, limitedVarCnts.get(new_vid) + 1);
                }
            }
        }

        this.fingerprint = new Fingerprint(this.structure);
        this.fingerprintCache.add(fingerprint);
    }

    /**
     * A copy constructor.
     */
    public Rule(Rule another) {
        this.fingerprintCache = another.fingerprintCache;
        this.category2TabuSetMap = another.category2TabuSetMap;
        this.structure = new ArrayList<>(another.structure.size());
        for (Predicate predicate: another.structure) {
            this.structure.add(new Predicate(predicate));
        }
        this.limitedVarCnts = new ArrayList<>(another.limitedVarCnts);
        this.length = another.length;
        this.eval = another.eval;
        this.fingerprint = another.fingerprint;
    }

    /**
     * A clone method that correctly uses the copy constructor of any sub-class. The copy constructor may not be useful,
     * as a super class copy constructor generates only the super class object even on the sub-class object.
     *
     * @return A copy of the rule instance
     */
    public abstract Rule clone();

    /**
     * Check if the rule structure is invalid. The structure is invalid if:
     *   1. It contains duplicated predicates Todo: Partial duplication with the head should not be defined as invalid.
     *   2. It contains independent Fragment
     */
    protected boolean isInvalid() {
        /* Check independent fragment (may happen when looking for generalizations) */
        /* Use the disjoint set: join limited variables if they are in the same predicate */
        /* Assumption: no body predicate contains only empty or constant argument */
        DisjointSet disjoint_set = new DisjointSet(usedLimitedVars());

        /* Check duplicated predicates */
        /* 1. Check by set */
        /* 2. A more aggressive check to prevent duplication with the head: check for body predicate with same argument
           with the head */
        Predicate head_pred = structure.get(HEAD_PRED_IDX);
        {
            /* Put the LVs in the head to the disjoint set */
            List<Integer> lv_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                int argument = head_pred.args[arg_idx];
                if (Argument.isVariable(argument)) {
                    lv_ids.add(Argument.decode(argument));
                }
            }
            if (lv_ids.isEmpty()) {
                if (structure.size() >= 2) {
                    /* The body is an independent fragment if the head contains no LV while body is not empty */
                    return true;
                }
            } else {
                /* Must check here because there may be no LV in the head */
                int first_id = lv_ids.get(0);
                for (int i = 1; i < lv_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, lv_ids.get(i));
                }
            }
        }

        Set<Predicate> predicate_set = new HashSet<>();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            Predicate body_pred = structure.get(pred_idx);
            if (head_pred.functor == body_pred.functor) {
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    int head_arg = head_pred.args[arg_idx];
                    int body_arg = body_pred.args[arg_idx];
                    if (Argument.isNonEmpty(head_arg) && head_arg == body_arg) {
                        return true;
                    }
                }
            }

            boolean args_complete = true;
            List<Integer> lv_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                int argument = body_pred.args[arg_idx];
                if (Argument.isEmpty(argument)) {
                    args_complete = false;
                } else if (Argument.isVariable(argument)) {
                    lv_ids.add(Argument.decode(argument));
                }
            }

            if (args_complete) {
                if (!predicate_set.add(body_pred)) {
                    return true;
                }
            }

            /* 在同一个Predicate中出现的Bounded Var合并到一个集合中 */
            /* Join the LVs in the same predicate */
            if (lv_ids.isEmpty()) {
                /* If no LV in body predicate, the predicate is certainly part of the fragment */
                return true;
            } else {
                int first_id = lv_ids.get(0);
                for (int i = 1; i < lv_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, lv_ids.get(i));
                }
            }
        }

        /* Check for independent fragment */
        return 2 <= disjoint_set.totalSets();
    }

    /**
     * Check if the rule structure has already been verified and added to the cache. If not, add the rule fingerprint to
     * the cache.
     */
    protected boolean cacheHit() {
        return !fingerprintCache.add(fingerprint);
    }

    /**
     * Check if the rule structure should be pruned by the tabu set.
     */
    protected boolean tabuHit() {
        for (int subset_size = 0; subset_size < structure.size(); subset_size++) {
            for (MultiSet<Integer> category_subset : categorySubsets(subset_size)) {
                final Set<Fingerprint> tabu_set = category2TabuSetMap.get(category_subset);
                if (null == tabu_set) continue;
                for (Fingerprint rfp : tabu_set) {
                    if (rfp.generalizationOf(this.fingerprint)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Enumerate the subcategories of the rule of a certain size.
     *
     * @param subsetSize The size of the subcategories
     * @return A set of subcategories
     */
    protected Set<MultiSet<Integer>> categorySubsets(int subsetSize) {
        final Set<MultiSet<Integer>> subsets = new HashSet<>();
        if (0 == subsetSize) {
            subsets.add(new MultiSet<>());
        } else {
            templateSubsetsHandler(subsets, new Integer[subsetSize], subsetSize - 1, FIRST_BODY_PRED_IDX);
        }
        return subsets;
    }

    /**
     * Handler for enumerating the subcategories.
     */
    protected void templateSubsetsHandler(Set<MultiSet<Integer>> subsets, Integer[] template, int depth, int startIdx) {
        if (0 < depth) {
            for (int pred_idx = startIdx; pred_idx < structure.size(); pred_idx++) {
                template[depth] = structure.get(pred_idx).functor;
                templateSubsetsHandler(subsets, template, depth-1, pred_idx+1);
            }
        } else {
            for (int pred_idx = startIdx; pred_idx < structure.size(); pred_idx++) {
                template[depth] = structure.get(pred_idx).functor;
                subsets.add(new MultiSet<>(template));
            }
        }
    }

    /**
     * Calculate the evaluation of the rule.
     */
    protected abstract Eval calculateEval();

    /**
     * Check if the coverage of the rule is below the threshold. If so, add the fingerprint to the tabu set.
     */
    protected boolean insufficientCoverage() {
        if (MIN_FACT_COVERAGE >= recordCoverage()) {
            add2TabuSet();
            return true;
        }
        return false;
    }

    /**
     * Calculate the record coverage of the rule.
     */
    protected abstract double recordCoverage();

    /**
     * Add this rule to the tabu set
     */
    protected void add2TabuSet() {
        final MultiSet<Integer> functor_mset = new MultiSet<>();
        for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            functor_mset.add(structure.get(pred_idx).functor);
        }
        final Set<Fingerprint> tabu_set = category2TabuSetMap.computeIfAbsent(
                functor_mset, k -> new HashSet<>()
        );
        tabu_set.add(fingerprint);
    }

    /**
     * Specialization Case 1: Convert a UV to an existing LV.
     *
     * @param predIdx The index of the predicate
     * @param argIdx The index of the argument
     * @param varId The id of the LV
     * @return The update status
     */
    public UpdateStatus cvt1Uv2ExtLv(final int predIdx, final int argIdx, final int varId) {
        cvt1Uv2ExtLvUpdStrc(predIdx, argIdx, varId);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt1Uv2ExtLvHandlerPreCvg(predIdx, argIdx, varId);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt1Uv2ExtLvHandlerPostCvg(predIdx, argIdx, varId);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 1.
     */
    protected void cvt1Uv2ExtLvUpdStrc(final int predIdx, final int argIdx, final int varId) {
        final Predicate target_predicate = structure.get(predIdx);
        target_predicate.args[argIdx] = Argument.variable(varId);
        limitedVarCnts.set(varId, limitedVarCnts.get(varId)+1);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 1, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(final int predIdx, final int argIdx, final int varId);

    /**
     * Handler for case 1, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(final int predIdx, final int argIdx, final int varId);

    /**
     * Specialization Case 2: Add a new predicate and convert a UV in the new predicate to an existing LV.
     *
     * @param functor The functor of the new predicate
     * @param arity The arity of the new predicate
     * @param argIdx The index of the argument
     * @param varId The id of the LV
     * @return The update status
     */
    public UpdateStatus cvt1Uv2ExtLv(final int functor, final int arity, final int argIdx, final int varId) {
        cvt1Uv2ExtLvUpdStrc(functor, arity, argIdx, varId);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt1Uv2ExtLvHandlerPreCvg(structure.get(structure.size()-1), argIdx, varId);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt1Uv2ExtLvHandlerPostCvg(structure.get(structure.size()-1), argIdx, varId);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 2.
     */
    protected void cvt1Uv2ExtLvUpdStrc(final int functor, final int arity, final int argIdx, final int varId) {
        final Predicate target_predicate = new Predicate(functor, arity);
        structure.add(target_predicate);
        target_predicate.args[argIdx] = Argument.variable(varId);
        limitedVarCnts.set(varId, limitedVarCnts.get(varId)+1);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 2, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(
            final Predicate newPredicate, final int argIdx, final int varId
    );

    /**
     * Handler for case 2, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(
            final Predicate newPredicate, final int argIdx, final int varId
    );

    /**
     * Specialization Case 3: Convert 2 UVs in the rule to a new LV.
     *
     * @param predIdx1 The index of the first predicate
     * @param argIdx1 The argument index in the first predicate
     * @param predIdx2 The index of the second predicate
     * @param argIdx2 The argument index in the second predicate
     * @return The update status
     */
    public UpdateStatus cvt2Uvs2NewLv(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        cvt2Uvs2NewLvUpdStrc(predIdx1, argIdx1, predIdx2, argIdx2);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt2Uvs2NewLvHandlerPreCvg(predIdx1, argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt2Uvs2NewLvHandlerPostCvg(predIdx1, argIdx1, predIdx2, argIdx2);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 3.
     */
    protected void cvt2Uvs2NewLvUpdStrc(final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2) {
        final Predicate target_predicate1 = structure.get(predIdx1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final int new_var = Argument.variable(limitedVarCnts.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        limitedVarCnts.add(2);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 3, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Handler for case 3, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Specialization Case 4: Add a new predicate to the rule and convert 2 UVs in the rule to a new LV. Note: Exactly one of the
     * selected arguments are from the newly added predicate. 
     *
     * @param functor The functor of the new predicate
     * @param arity The arity of the new predicate
     * @param argIdx1 The argument index in the first predicate
     * @param predIdx2 The index of the second predicate
     * @param argIdx2 The argument index in the second predicate
     * @return The update status
     */
    public UpdateStatus cvt2Uvs2NewLv(
            final int functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        cvt2Uvs2NewLvUpdStrc(functor, arity, argIdx1, predIdx2, argIdx2);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt2Uvs2NewLvHandlerPreCvg(structure.get(structure.size()-1), argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt2Uvs2NewLvHandlerPostCvg(structure.get(structure.size()-1), argIdx1, predIdx2, argIdx2);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 4.
     */
    protected void cvt2Uvs2NewLvUpdStrc(
            final int functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        final Predicate target_predicate1 = new Predicate(functor, arity);
        structure.add(target_predicate1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final int new_var = Argument.variable(limitedVarCnts.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        limitedVarCnts.add(2);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 4, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Handler for case 4, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Specialization Case 5: Convert a UV to a constant.
     *
     * @param predIdx The index of the predicate
     * @param argIdx The index of the argument
     * @param constant The numeration of the constant symbol
     * @return The update status
     */
    public UpdateStatus cvt1Uv2Const(final int predIdx, final int argIdx, final int constant) {
        cvt1Uv2ConstUpdStrc(predIdx, argIdx, constant);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt1Uv2ConstHandlerPreCvg(predIdx, argIdx, constant);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt1Uv2ConstHandlerPostCvg(predIdx, argIdx, constant);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 5.
     */
    protected void cvt1Uv2ConstUpdStrc(final int predIdx, final int argIdx, final int constant) {
        final Predicate predicate = structure.get(predIdx);
        predicate.args[argIdx] = Argument.constant(constant);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 5, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ConstHandlerPreCvg(final int predIdx, final int argIdx, final int constant);

    /**
     * Handler for case 5, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ConstHandlerPostCvg(final int predIdx, final int argIdx, final int constant);

    /**
     * Generalization: Remove an assignment to the argument.
     *
     * @param predIdx The index of the predicate
     * @param argIdx The index of the argument
     * @return The update status
     */
    public UpdateStatus rmAssignedArg(final int predIdx, final int argIdx) {
        rmAssignedArgUpdStrc(predIdx, argIdx);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = rmAssignedArgHandlerPreCvg(predIdx, argIdx);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = rmAssignedArgHandlerPostCvg(predIdx, argIdx);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for generalization.
     */
    protected void rmAssignedArgUpdStrc(final int predIdx, final int argIdx) {
        final Predicate predicate = structure.get(predIdx);
        final int removed_argument = predicate.args[argIdx];
        predicate.args[argIdx] = Argument.EMPTY_VALUE;

        /* Rearrange the var ids if the removed argument is an LV */
        if (Argument.isVariable(removed_argument)) {
            int removed_var_id = Argument.decode(removed_argument);
            final Integer var_uses_cnt = limitedVarCnts.get(removed_var_id);
            if (2 >= var_uses_cnt) {
                /* The LV should be removed */
                /* Change the id of the latest LV to the removed one */
                /* Note that the removed one may also be the latest LV */
                int latest_var_id = limitedVarCnts.size() - 1;
                limitedVarCnts.set(removed_var_id, limitedVarCnts.get(latest_var_id));
                limitedVarCnts.remove(latest_var_id);

                /* Clear the argument as well as the UV due to the remove (if applicable) */
                boolean found = false;
                for (Predicate another_predicate : structure) {
                    for (int i = 0; i < another_predicate.arity(); i++) {
                        if (removed_argument == another_predicate.args[i]) {
                            another_predicate.args[i] = Argument.EMPTY_VALUE;
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }

                int latest_var = Argument.variable(latest_var_id);
                if (removed_argument != latest_var) {
                    /* Rewrite the vars to the removed one */
                    for (Predicate another_predicate : structure) {
                        for (int i = 0; i < another_predicate.arity(); i++) {
                            another_predicate.args[i] = (latest_var == another_predicate.args[i]) ?
                                    removed_argument : another_predicate.args[i];
                        }
                    }
                }
            } else {
                /* Remove the occurrence only */
                limitedVarCnts.set(removed_var_id, var_uses_cnt - 1);
            }
        }
        length--;

        /* The removal may result in a predicate where all arguments are empty, remove the predicate if it is not the head */
        Iterator<Predicate> itr = structure.iterator();
        itr.next();   // Skip the head
        while (itr.hasNext()) {
            Predicate body_pred = itr.next();
            boolean is_empty_pred = true;
            for (int argument: body_pred.args) {
                if (Argument.isNonEmpty(argument)) {
                    is_empty_pred = false;
                    break;
                }
            }
            if (is_empty_pred) {
                itr.remove();
            }
        }
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for generalization, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus rmAssignedArgHandlerPreCvg(final int predIdx, final int argIdx);

    /**
     * Handler for generalization, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus rmAssignedArgHandlerPostCvg(final int predIdx, final int argIdx);

    /**
     * Calculate the evidence of positively entailed facts in the KB. Each piece of evidence is an ordered array of
     * predicates, where the head is the first.
     *
     * @return The evidence generated by the rule.
     */
    public abstract EvidenceBatch getEvidenceAndMarkEntailment();

    /**
     * Calculate the counterexamples generated by the rule.
     *
     * @return The set of counterexamples.
     */
    public abstract Set<Record> getCounterexamples();

    public Predicate getPredicate(int idx) {
        return structure.get(idx);
    }

    public Predicate getHead() {
        return structure.get(HEAD_PRED_IDX);
    }

    public int length() {
        return length;
    }

    public int usedLimitedVars() {
        return limitedVarCnts.size();
    }

    public int predicates() {
        return structure.size();
    }

    public Eval getEval() {
        return eval;
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public String toString(NumerationMap map) {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(structure.get(0).toString(map)).append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1).toString(map));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString(map));
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(structure.get(0).toString()).append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1).toString());
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString());
            }
        }
        return builder.toString();
    }

    public String toDumpString(NumerationMap map) {
        StringBuilder builder = new StringBuilder(structure.get(0).toString(map));
        builder.append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1).toString(map));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString(map));
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return this.fingerprint.equals(that.fingerprint);
    }

    @Override
    public int hashCode() {
        return fingerprint.hashCode();
    }
}
