"""
Decision forests analytics module.

This module contains routines which extract various summary data from
decision forests, like the most important features and the most important
rule chains.

You are expected to import this module in ipython and call some of the
public routines:

    >>> %load_ext autoreload
    >>> %autoreload 2
    >>> from forest_analysis import *
    >>> import joblib
    >>> cl = joblib.load("/tmp/GBC.pkl")
    >>> feats_by_importance(cl)
    >>> rulechains_by_significance(cl)
"""

from collections import namedtuple
import numpy as np
from sklearn.tree import _tree


def feats_by_importance(cl):
    """
    List features ordered by importance.
    """
    cfier, labels = cl

    feature_importance = cfier.feature_importances_
    # make importances relative to max importance
    feature_importance = 100.0 * (feature_importance / feature_importance.max())
    sorted_idx = np.argsort(feature_importance)

    return [(labels[i], feature_importance[i]) for i in sorted_idx]


def rulechains_by_significance(cl):
    """
    List all rule chains (paths from root to leaf), ordered by their
    significance

        significance = |samples * value|

    (we dreamed up this measure).
    """
    cfier, labels = cl
    Chain = namedtuple('Chain', ["conditions", "significance", "value", "samples", "impurity"])

    def gen_chains(tree):
        # IChain is list of node indices on a root-leaf path
        q_ichains = [[(0,)]]
        while q_ichains:
            ichain = q_ichains.pop()
            last_i = ichain[-1][0]
            if tree.children_left[last_i] == _tree.TREE_LEAF:
                value = tree.value[last_i]
                if tree.n_outputs == 1:
                    value = value[0, 0]
                yield Chain(["%s %s %.2f" % (labels[tree.feature[inode]], comp, tree.threshold[inode]) for inode, comp in ichain[:-1]],
                            abs(tree.n_node_samples[last_i] * value),
                            value, tree.n_node_samples[last_i],
                            tree.impurity[last_i])
            else:
                q_ichains.append(ichain[:-1] + [(last_i, '<='), (tree.children_left[last_i],)])
                q_ichains.append(ichain[:-1] + [(last_i, '>'), (tree.children_right[last_i],)])

    chains = []
    for est in cfier.estimators_:
        chains += list(gen_chains(est[0].tree_))

    chains.sort(key=lambda x: x.significance)
    return chains
