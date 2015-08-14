"""
This module defines export functions for decision trees and forests.
"""

# Based on the export_graphviz module.
# Authors: Gilles Louppe <g.louppe@gmail.com>
#          Peter Prettenhofer <peter.prettenhofer@gmail.com>
#          Brian Holt <bdholt1@gmail.com>
#          Noel Dawe <noel@dawe.me>
#          Satrajit Gosh <satrajit.ghosh@gmail.com>
# Licence: BSD 3 clause

from sklearn.externals import six

from sklearn.tree import _tree


def export_graphviz(decision_trees, out_file="tree.dot", feature_names=None,
                    max_depth=None):
    """Export decision trees in DOT format.

    This function generates a GraphViz representation of the decision trees,
    which is then written into `out_file`. Once exported, graphical renderings
    can be generated using, for example::

        $ dot -Tps tree.dot -o tree.ps      (PostScript format)
        $ dot -Tpng tree.dot -o tree.png    (PNG format)

    The sample counts that are shown are weighted with any sample_weights that
    might be present.

    Parameters
    ----------
    decision_tree : decision tree classifier
        The decision tree to be exported to GraphViz.

    out_file : file object or string, optional (default="tree.dot")
        Handle or name of the output file.

    feature_names : list of strings, optional (default=None)
        Names of each of the features.

    max_depth : int, optional (default=None)
        The maximum depth of the representation. If None, the tree is fully
        generated.

    Examples
    --------
    >>> from sklearn.datasets import load_iris
    >>> from sklearn import tree

    >>> clf = tree.DecisionTreeClassifier()
    >>> iris = load_iris()

    >>> clf = clf.fit(iris.data, iris.target)
    >>> tree.export_graphviz(clf,
    ...     out_file='tree.dot')                # doctest: +SKIP
    """
    def node_to_str(tree, node_id, criterion):
        if not isinstance(criterion, six.string_types):
            criterion = "impurity"

        value = tree.value[node_id]
        if tree.n_outputs == 1:
            value = value[0, :]

        if tree.children_left[node_id] == _tree.TREE_LEAF:
            return "%s = %.4f\\nsamples = %s\\nvalue = %s" \
                   % (criterion,
                      tree.impurity[node_id],
                      tree.n_node_samples[node_id],
                      value)
        else:
            if feature_names is not None:
                feature = feature_names[tree.feature[node_id]]
            else:
                feature = "X[%s]" % tree.feature[node_id]

            return "%s <= %.4f\\n%s = %s\\nsamples = %s" \
                   % (feature,
                      tree.threshold[node_id],
                      criterion,
                      tree.impurity[node_id],
                      tree.n_node_samples[node_id])

    def fill_node_attrs(color):
        if color < 0:
            return ''
        elif color > 0:
            return ', style="filled", fillcolor="plum"'
        else:
            return ', style="filled", fillcolor="cornflowerblue"'

    def recurse(tree, node_id, criterion, parent=None, depth=0, node_id_offset=0, tree_color=-1):
        if node_id == _tree.TREE_LEAF:
            raise ValueError("Invalid node_id %s" % _tree.TREE_LEAF)

        left_child = tree.children_left[node_id]
        right_child = tree.children_right[node_id]

        # Add node with description
        if max_depth is None or depth <= max_depth:
            if left_child != _tree.TREE_LEAF:
                node_attrs = fill_node_attrs(tree_color)
            else:
                node_attrs = fill_node_attrs(-1)

            out_file.write('%d [label="%s", shape="box"%s] ;\n' %
                           (node_id + node_id_offset, node_to_str(tree, node_id, criterion), node_attrs))

            if parent is not None:
                # Add edge to parent
                out_file.write('%d -> %d ;\n' % (parent + node_id_offset, node_id + node_id_offset))

            if left_child != _tree.TREE_LEAF:
                recurse(tree, left_child, criterion=criterion, parent=node_id,
                        depth=depth + 1, node_id_offset=node_id_offset, tree_color=tree_color)
                recurse(tree, right_child, criterion=criterion, parent=node_id,
                        depth=depth + 1, node_id_offset=node_id_offset, tree_color=tree_color)

        else:
            out_file.write('%d [label="(...)", shape="box"%s] ;\n' % (node_id + node_id_offset, fill_node_attrs(tree_color)))

            if parent is not None:
                # Add edge to parent
                out_file.write('%d -> %d ;\n' % (parent + node_id_offset, node_id + node_id_offset))

    own_file = False
    try:
        if isinstance(out_file, six.string_types):
            if six.PY3:
                out_file = open(out_file, "w", encoding="utf-8")
            else:
                out_file = open(out_file, "wb")
            own_file = True

        out_file.write("digraph Tree {\n")

        node_id_offset = 0

        try:
            # XXX: Is there a more fancy way to check the input is a list?
            len(decision_trees)
            tree_color = 0
        except:
            decision_trees = [decision_trees]
            tree_color = -1

        for decision_tree in decision_trees:
            if isinstance(decision_tree, _tree.Tree):
                recurse(decision_tree, 0, criterion="impurity", node_id_offset=node_id_offset, tree_color=tree_color)
                node_id_offset += decision_tree.node_count
            else:
                recurse(decision_tree.tree_, 0, criterion=decision_tree.criterion, node_id_offset=node_id_offset, tree_color=tree_color)
                node_id_offset += decision_tree.tree_.node_count
            tree_color = 1 - tree_color

        out_file.write("}")

    finally:
        if own_file:
            out_file.close()
