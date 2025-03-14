// package com.ddfinv.core.rustfiles;

// public class DataStructures {
// }



/*


// imports the Box struct from the standard library boxed module
use std::boxed::Box;

// imports all public items from the firm_models module
use crate::{errors::ApplicationError, firm_models::*};
// imports all public items from the errors module
//use crate::errors::*;
//
// ********************************************
// data_structures.rs module definitions begin here:
// ******************************************** ;
//

/// Represents our avl tree structure, stores nodes with data elements type T
///
/// Greater structure used to house the AVL tree. Is composed of Node structs, which
/// may branch out. Each node potentially possesses 2 child nodes, left and right.
///
///# Fields
///
///* `root: Option<Box<Node<T>>>` - optional Box containing Node<T>
///
pub struct AVLTree<T> {
    // optional box containing an implemented node<T> struct
    root: Option<Box<Node<T>>>,
}

/// Represents the node struct for the tree
///
/// The node structure of which the AVL tree is composed. Each
/// node contains a left and right child (if they exist), as well
/// as the height value used to maintain balance within the tree.
/// Each node contains a type T data object which is used to store
/// each Client struct inserted into the tree, on a 1:1 ratio with
/// each Client receiving their own node.
///
///# Fields
///
///* `data: T` - the type T data object stored within a Node
///* `left: Option<Box<Node<T>>>` - Optional Box containing the
///                 left child of current node if existing
///* `right: Option<Box<Node<T>>>` - Optional Box containing the
///                 right child of current node if existing
///* `height: i32` - the i32 integer height value for each node in the tree
///
#[derive(Clone)] // clone added to facilitate balancing / node clones
pub struct Node<T> {
    // generic / template type T data object - (Client object here)
    data: T,
            // optional Box Node ptr type T for left child tree
            left: Option<Box<Node<T>>>,
    // optional Box Node ptr type T for right child tree
    right: Option<Box<Node<T>>>,
    height: i32,
}

impl<T: Ord + Clone + Identification + std::fmt::Debug> AVLTree<T> {
    ///AVL tree constructor function to create new, empty tree
    ///
    /// function creates an new, empty implemention of the AVL tree struct
    ///
    ///
    ///# Returns
    ///
    ///* 'Self' - this implemented struct object instance, AvlTree<T>
    ///
    pub fn new() -> Self {
        AVLTree { root: None }
    }
    /// Checks the tree to see if it is empty
    ///
    /// Determines whether or not the AVLTree is empty,
    /// or if it contains any nodes. Uses the is_none()
    /// method on the root node to determine if it has any branches.
    ///
    ///# Arguments
    ///
    ///* '&self' - reference to self (this implemented AVLTree struct instance)
    ///
    ///# Returns
    ///
    ///* 'bool' - returns true if the is_none evaluation is valid
    ///* 'bool' - returns false when is_none check is not valid (is something)
    ///
    pub fn is_empty(&self) -> bool {
        self.root.is_none()
    }
    /// Destructor / removal function for the AVLTree
    ///
    /// this method purges and clears all nodes / elements
    /// from the AVLTree. Assigns the root node the value None, this
    /// removes / forces all data in the tree to become unreachable
    /// and out of scope. Once the data is out of reach, rust's
    /// memory management / the drop checker cleans up any allocated
    /// memory.
    ///
    ///# Arguments
    ///
    ///* '&mut self' - mutable reference to self (implemented AVLTree instance)
    ///
    ///
    ///# Note
    ///
    ///* Documentation tests may be executed while cd'd into project directory
    ///  with the command: "cargo test" in console.
    ///
    ///# Examples
    ///
    ///```
    /// let mut avl_tree =  AVLTree::new();
    /// avl_tree.insert(5);
    /// avl_tree.insert(10);
    /// avl_tree.clear();
    /// assert!(avl_tree.is_empty());
    ///```
    ///
    pub fn clear(&mut self) {
        self.root = None;
    }
    /// In order traversal initiation method
    ///
    /// initiates the in order traversal using the root node
    /// of the AVLTree.
    ///
    ///# Arguments
    ///
    ///* '&self' - reference to self (this implemented instance of AVLTree)
    ///
    pub fn in_order(&self) {
        Self::in_order_traverse(&self.root);
    }
    /// In order traversal method logic implementation
    ///
    /// Performs recursive in order traversal of this AVLTree.
    /// Effectively prints the data of the tree in order, from smallest
    /// value, to the greatest value (index value / client_id).
    ///
    ///# Arguments
    ///
    ///* 'node: &Option<Box<Node<T>>>' - current node the traversal
    ///     method is processing. Used to print data & call the both the
    ///     closest smaller & closest larger child nodes.
    ///
    ///# Behavior
    ///
    /// 1. Call this function recursively with the left child of this
    ///     node (closest smaller value).
    /// 2. Print the data of this node out.
    /// 3. Call this function recursively with the right child of this
    ///     node (closest larger value).
    ///
    fn in_order_traverse(node: &Option<Box<Node<T>>>) {
        if let Some(node) = node {
            Self::in_order_traverse(&node.left);
            println!("{:?}", node.data);
            Self::in_order_traverse(&node.right);
        }
    }
    /// Returns the height of the selected node
    ///
    ///# Arguments
    ///
    ///* 'node: &Option<Node<T>>' - reference to optional Node<T>
    ///         that we are calculating height for
    ///
    ///# Returns
    ///
    ///* 'i32' - the calculated height of subtree using provided node
    ///
    ///# Behavior
    ///
    /// 1. .as_ref - we are turning option reference to the Box<Node<T>> reference,
    ///         this is done to perform height operations without need to take
    ///         ownership of the Box<node
    /// 2. .map_or combines the map method and the unwrap or method.
    ///         takes two args, the default value, as well as closure
    ///         when optional value is None, returns default value
    ///         when optional value is Some, returns result of closure operations
    /// 3.  Closure operations occur when map or = Some. defines optional value
    ///         or (&Box<Node<T>>) as n, n.height is us accessing the value for height
    ///         of the particular node that is passed as arg
    ///
    fn height(node: &Option<Box<Node<T>>>) -> i32 {
        node.as_ref().map_or(0, |n| n.height)
    }
    ///Calculates the balance factor for a given node
    ///
    ///The balance factor is the difference betweer the height
    ///of the left subtree, and the height of the right subtree
    ///
    ///# Arguments
    ///
    ///* 'node: &Node<T>' - reference to Node<T> we are calculating
    ///         the balance factor of.
    ///
    ///# Returns
    ///
    ///* 'i32' - the integer value that is the calculated balance factor
    ///
    fn balance_factor(node: &Node<T>) -> i32 {
        Self::height(&node.left) - Self::height(&node.right)
    }
    ///Performs balancing operations for a node
    ///
    /// determines if a subtree is in need of balancing. Typically
    /// used after an insertion or removal operation, and used
    /// to return the tree to a balanced state after modifications.
    ///
    ///# Arguments
    ///
    ///* 'mut node: Box<Node<T>>' - mutable node that is initial root
    ///         of a subtree & focus of rebalancing operations.
    ///
    ///# Returns
    ///
    ///* 'Box<Node<T>>' -  The updated subtree after balancing operations
    ///
    ///# Behavior
    ///
    ///* 1. This function call begins by performing a calculation of the current
    ///     node being used to call the function.
    ///* 2. The balance factor is then calculated for this node, using the now
    ///     updated height value for it.
    ///* 3. We then use conditional branching to manage the various rotations or
    ///     balancing operations that may be needed for a particular subtree
    ///* 4. If the balance factor for the node parameter is greater than 1 (2>=),
    ///     - we check to see if the left child node, left subtree is heavy OR if
    ///       the subtree is balanced ( = 0)
    ///         - when yes left subtree heavy OR balanced (=0),
    ///           - then perform right rotation on the node
    ///     - else, the left child's right subtree is heavy,
    ///         - we perform a left-right rotation
    ///* 5. Else if, the balance factor of param node is less than -1 ( <= -2)
    ///     - check if the right child node's right subtree is right heavy OR if
    ///       the subtree is balanced ( = 0)
    ///         - when yes the right subtree is heavy or balanced
    ///            - perform a left rotation on the subtree
    ///     - else, the right child node's left subtree is heavy
    ///         - perform a right, left rotation
    ///* 6. Return the new, updated and balance node + it's subtree
    ///
    // isolated logic used in insert/remove method to return tree balance
    // no longer duplicated in both location
    fn find_new_balance(mut node: Box<Node<T>>) -> Box<Node<T>> {
            node.height = 1 + std::cmp::max(Self::height(&node.left), Self::height(&node.right));
    let balance = Self::balance_factor(&node);

    if balance > 1 {
        if Self::balance_factor(node.left.as_ref().unwrap()) >= 0 {
            node = Self::right_rotate(node);
        } else {
            node.left = Some(Self::left_rotate(node.left.take().unwrap()));
            node = Self::right_rotate(node);
        }
    } else if balance < -1 {
        if Self::balance_factor(node.right.as_ref().unwrap()) <= 0 {
            node = Self::left_rotate(node);
        } else {
            node.right = Some(Self::right_rotate(node.right.take().unwrap()));
            node = Self::left_rotate(node);
        }
    }
    node // return
    }
    ///Performs a right rotation of the selected node
    ///
    ///Right rotation operation used in the process of
    ///returning the AVL tree to a balanced state.
    ///
    ///# Arguments
    ///
    ///* 'mut child_node: Box<Node<T>>' - the parent node we are
    ///         targeting for the rotation to the right
    ///
    ///# Returns
    ///
    ///* 'Box<Node<T>>' - returns the new root of updated subtree after rotation
    ///
    ///# Behavior
    ///
    ///* 1. We are taking the left child node of the child node parameter, and then
    ///     making it the new value for parent node
    ///* 2. Then we take the right child node of parent node, and store it as the
    ///     temporary node
    ///* 3. We then clone the necessary data objects to avoid the need to manage
    ///     the data's lifetimes.
    ///* 4. Next we begin restructuring / balancing
    ///     - we set left child of the child_node param as the node we previously
    ///     assigned to temp
    ///     - we then create a new right child node of parent using node constructor
    ///     with the data that child_node contains, and we assign the new positions
    ///     of it's children
    ///* 5. We then update the height values of the new subtrees
    ///     - first update the height for new right child of parent_node (data
    ///     from prev child_node)
    ///     - then we set the height of the parent node
    ///* 6. Return parent_node
    ///
    fn right_rotate(mut child_node: Box<Node<T>>) -> Box<Node<T>> {
            let mut parent_node = child_node.left.take().unwrap();
    let temp_node = parent_node.right.take();

    // clone data before performing movements to avoid upsetting borrow checker
    let child_data = child_node.data.clone();
    let child_right = child_node.right.take();

    child_node.left = temp_node;
    parent_node.right = Some(Box::new(Node {
        data: child_data,
                left: child_node.left.take(),
                right: child_right,
                height: child_node.height,
    }));

    // updating height
    if let Some(right) = &mut parent_node.right {
        right.height = 1 + std::cmp::max(Self::height(&right.left), Self::height(&right.right));
    }
    parent_node.height = 1 + std::cmp::max(
            Self::height(&parent_node.left),
    Self::height(&parent_node.right),
        );
    parent_node
    }
    ///Performs a left rotation of the selected node
    ///
    ///Leftward rotation operation used in the process of
    ///returning the AVL tree to a balanced state.
    ///
    ///# Arguments
    ///
    ///* 'mut parent_node: Box<Node<T>>' - the parent node we are
    ///         targeting for the leftward rotation
    ///
    ///# Returns
    ///
    ///* 'Box<Node<T>>' - returns the updated subtree, (new subtree root)
    ///
    ///# Behavior
    ///
    ///* 1. We are taking the right child node of the parent node parameter, and  then making it the
    ///     new value for child_node
    ///* 2. Then we take the left child node of child node, and store it as the temporary node
    ///* 3. We then clone the necessary data objects to avoid the need to manage
    ///     the data's lifetimes.
    ///* 4. Next we begin restructuring / balancing
    ///     - we set right child of the parent node param as the node we previously assigned to temp
    ///     - we then create a new left child node of child_node using node constructor with the
    ///       data that parent_node contains, and we assign the new positions of it's children
    ///* 5. We then update the height values of the new subtrees
    ///     - first update the height for new left child of child_node (data from prev parent_node)
    ///     - then we set the height of the child_node
    ///* 6. Return child_node
    ///
    fn left_rotate(mut parent_node: Box<Node<T>>) -> Box<Node<T>> {
            let mut child_node = parent_node.right.take().unwrap();
    let temp_node = child_node.left.take();

    // Clone necessary data
    let parent_data = parent_node.data.clone();
    let parent_left = parent_node.left.take();

    parent_node.right = temp_node;
    child_node.left = Some(Box::new(Node {
        data: parent_data,
                left: parent_left,
                right: parent_node.right.take(),
                height: parent_node.height,
    }));

    // updating height
    if let Some(left) = &mut child_node.left {
        left.height = 1 + std::cmp::max(Self::height(&left.left), Self::height(&left.right));
    }
    child_node.height = 1 + std::cmp::max(
            Self::height(&child_node.left),
    Self::height(&child_node.right),
        );
    child_node
    }
    /// Initiates search to find a node
    ///
    /// Uses a provided key value to call the
    /// find function implementation that will recursively
    /// search through the tree for a matching key value.
    ///
    ///# Arguments
    ///
    ///* '&self' - a reference to self, the AVLTree<T>
    ///* 'key: i32' - the provided key value used to search
    ///         through the keys values of AVLTree nodes
    ///
    ///# Returns
    ///
    ///* 'Result<&T, ApplicationError>' - The desired data object, or an Application error
    ///* if match found, returns the data object stored in the node (a Client)
    ///* if no match found, returns ApplicationError:NoMatch & appropriate error message
    ///
    pub fn find(&self, key: i32) -> Result<&T, ApplicationError> {
            self.find_value(&self.root, key).ok_or_else(|| {
            ApplicationError::NoMatchError(format!(
            "No client match found for the provided ID: {}",
            key
            ))
        })
    }
    /// Function implementing the method to find a specific value in tree
    ///
    /// Internal / private function that implements the logic to recursively
    /// search through the AVLTree<T> for a matching id/key value.
    /// O(log n) complexity due to tree balance, and match filtering logic.
    /// Match statement used to determine the pathing recursion takes by
    /// comparing values to their left/right child nodes after verifying
    /// current node is not the target.
    ///
    ///
    ///# Arguments
    ///
    ///* '&'a self - reference to lifetime of self (AVLTree<T>)
    ///* 'current_node: &'a Option<Box<Node<T>>>' - optional reference to the
    ///                 box containing the current node (with data)
    ///                 we are processing in the tree.
    ///* 'target_key: i32' - the key / id value we are searching the tree for
    ///
    ///# Returns
    ///
    ///* 'Option<&'a T>' - optional reference to data T object with lifetime
    ///     if Some - match found returns reference to the matching data object T
    ///     if None - no match found, returns nothing
    ///
    ///# Behavior
    ///
    ///* 1. We start with a check to sees whether current node is a leaf or
    ///     if the tree / subtree does not have any children (is empty)
    ///     - When None is the result, we return None (node not found)
    ///* 2. We then use match to current_node's data, and we compare the data
    ///     of it's key (client_id) against the parameter value for target key
    ///     - If the data values match, then we found the target node, return
    ///       Some, a reference to the data object that is stored within the node
    ///     - If the value is less than the current_node's key, then we recursively
    ///       call the left child node with this function to repeat the process with
    ///       left subtree.
    ///     - If the value is greater than, we do use the right subtree / child node
    ///       to recursively search for the matching node
    ///
    fn find_value<'a>(
            &'a self,
    current_node: &'a Option<Box<Node<T>>>,
    target_key: i32,
    ) -> Option<&'a T> {
    match current_node {
        None => None,
                Some(node) => match target_key.cmp(&node.data.get_key()) {
            // https://doc.rust-lang.org/std/cmp/enum.Ordering.html
            std::cmp::Ordering::Equal => Some(&node.data),
            std::cmp::Ordering::Less => self.find_value(&node.left, target_key),
            std::cmp::Ordering::Greater => self.find_value(&node.right, target_key),
        },
    }
}
/// Public access method for AVLTree insertion implementation
///
/// Provides access to the function that implements the insertion
/// logic for the AVLTree. Passes the data object to be inserted
///
///# Arguments
///
///* '&mut self' - mutable reference to self, the AVLTree
///* 'data: T' - data object T that is being inserted into
///         the AVLTree
///# Returns
///
///* 'Result<(), ApplicationError>' -
///     on success, returns Ok
///     on failure, returns Application error
///
pub fn insert(&mut self, data: T) -> Result<(), ApplicationError> {
match Self::insert_value(self.root.take(), data) {
Ok(new_root) => {
self.root = new_root;
Ok(())
        }
Err(e) => Err(e),
        }
                }
/// Private/internal logic implementation for inserting into AVLTree<T>
///
/// Takes the data object T, and locates it's sorted insertion point
/// within the balanced tree. When tree is empty, node = None, and a new
/// node is created with the data object, the new root node.
/// When not empty, begins the comparison / match statements used to find it's
/// insertion point. Uses the key value of data object T (client_id) to locate the

///
///# Arguments
///* 'node: Option<Box<Node<T>>>' - optional box containing the current node
///         if Some: the current node being used to recursively call insert
///         if None: root node is empty, no node exists to call insert with
///* 'data: T' - The data object being inserted into the tree (a Client)
///
///# Returns
///
///* 'Result<Option<Box<Node<T>>>, ApplicationError>' - result of insert_value operations
///     On success:
///         'Some(Box<Node<T>>)': updated subtree, after insertion complete & tree rebalanced
///     On failure:
///         'ApplicationError::InputError' a client already exists with that ID
///
///# Note
///
///function is concluded on successfully finding insert position by calling the find_new_balance()
///fn to calculate new height, and to balance the subtree branches (left/right rotations)
///
///# Behavior
///
///* 1. We start with a check to sees whether current node is a leaf or
///     if the tree / subtree does not have any children (is empty)
///     - When None is result, we create a new node with the data object from parameter
///       and then we return that Some (new node)
///* 2. If the node already exists at the position, we compare the key of the param data
///     object against the current_node's key (client_id)
///* 3. A match statement is used to process the comparison of these keys
///     - if the value from data is less that current node's key, we recursively call
///       the insert_value function with the current node's left child node position
///     - if the key value is greater than the current node's then we do the same,
///       calling the insert_value with the right child node.
///       if the values are equal, then we return an error, because we are attempting
///       to insert a data object with a duplicate key value.
///* 4. after the insertion process, we then call the find new balance function to update
///     the height and rebalance subtrees if necessary
///* 5. We then return the balanced, updated subtree post insertion
///
fn insert_value(
        node: Option<Box<Node<T>>>,
        data: T,
        ) -> Result<Option<Box<Node<T>>>, ApplicationError> {
match node {
None => Ok(Some(Box::new(Node {
    data,
            left: None,
            right: None,
            height: 1,
}))),
Some(mut node) => {
match data.get_key().cmp(&node.data.get_key()) {
// https://doc.rust-lang.org/std/cmp/enum.Ordering.html
std::cmp::Ordering::Less => {
node.left = Self::insert_value(node.left.take(), data)?;
        }
std::cmp::Ordering::Greater => {
node.right = Self::insert_value(node.right.take(), data)?;
        }
std::cmp::Ordering::Equal => {
        return Err(ApplicationError::InputError(format!(
        "Client already exists with the ID value: {}",
                   data.get_key()
                        )));
                                }
                                }
Ok(Some(Self::find_new_balance(node))) // return
        }
        }
        }
/// Provides public access to the node removal function
///
/// public access to the private/internal remove_node function.
/// calls the removal function using the target key value.
///
///# Arguments
///
///* '&mut self' - mutable reference to self (AVLTree)
///* 'key: i32' - the key value of node / data object (Client) to remove
///
///# Returns
///
///* 'Result<T, ApplicationError>'
///*    On success return:
///         'T' the value (Client object) from the deleted node
///*    On failure return:
///         'ApplicationError::NoMatchError' when no client for provided key found
///
pub fn remove(&mut self, key: i32) -> Result<T, ApplicationError> {
    let (new_root, deleted_value) = Self::remove_node(self.root.take(), key)?;
    self.root = new_root;
    deleted_value // return
            .ok_or_else(|| {
            ApplicationError::NoMatchError(format!(
            "No matching client found with ID value: {}",
            key
                ))
            })
}
/// Removes a node by the provided key value (if exists)
///
/// Recursively traverses the tree, searching for the node with
/// key value that matches argument. If it exists, removes the node,
/// and then rebalances the tree if the action would unbalance it.
///
///# Arguments
///
///* 'node: Option<Box<Node<T>>>' - the current node (root of this current subtree)
///* 'key: i32' - key value of target node to be removed (client_id)
///
///# Returns
///
///* 'Result<(Option<Box<Node<T>>>, Option<T>), ApplicationError>'
///     On successful operations, return one of:
///         'Option<Box<Node<T>>>' - Optional box containing Node<T> updated subtree
///             if some - updated / new subtree post removal and rebalancing operations
///             if none - node removed was a leaf with no subtree
///*        'option<t>' - optional value t (client object) if there was a match
///             if some - the value stored in the removed node (client object)
///             if None - should not occur, instead noMatchError occurs
///     On failure returns ApplicationError
///         Returns the NoMatchError with related error message
///
///# Behavior
///
///* 1. Start out by checking if the current node is empty, or if it is a leaf
///     without and children
///     - when None is returned it means that no match was found, so we return
///       a no match error
///* 2. When the node exists at the position, we compare the key of it against the key
///     passed in as an argument
///* 3. we then use a match statement to evaluate the comparisons
///     - When current node key is less than argument key value
///         call remove node on the left child / left subtree of node
///     - when current key is greater than the argument key
///         we call remove node of the right subtree
///     - when the key values are equal, we then move to delete processing
///       - if there are no children nodes, we do not need to worry about
///         repositioning the tree, return None and the data value
///     - when there is only a left child, return left child and deleted value
///     - when there is only a right child, return  the right child,
///       and the deleted value
///     - when there are two child nodes, find the min value from right subtree,
///       using remove min fn, replace the current_node's data with that value,
///       we are left with the new right value and successor
///* 4. After a successful removal, we need to adjust the tree balance, as it may
///     need to be balanced.
///* 5. return the new, balanced subtree
///
fn remove_node(
        node: Option<Box<Node<T>>>,
        key: i32,
        ) -> Result<(Option<Box<Node<T>>>, Option<T>), ApplicationError> {
match node {
None => Err(ApplicationError::NoMatchError(format!(
        "No client match was found for provided ID value: {}",
            key
            ))),
Some(mut node) => {
let (new_subtree, deleted_value) = match key.cmp(&node.data.get_key()) {
// https://doc.rust-lang.org/std/cmp/enum.Ordering.html
std::cmp::Ordering::Less => {
let (new_left, value) = Self::remove_node(node.left.take(), key)?;
node.left = new_left;
                        (Some(node), value)
        }
std::cmp::Ordering::Greater => {
let (new_right, value) = Self::remove_node(node.right.take(), key)?;
node.right = new_right;
                        (Some(node), value)
        }
std::cmp::Ordering::Equal => {
let deleted_value = Some(node.data.clone());
match (node.left.take(), node.right.take()) {
        (None, None) => (None, deleted_value),
        (Some(left), None) => (Some(left), deleted_value),
        (None, Some(right)) => (Some(right), deleted_value),
        (Some(left), Some(right)) => {
let (new_right, successor) = Self::remove_minimum(right)?;
node.data = successor;
node.left = Some(left);
node.right = new_right;
                                (Some(node), deleted_value)
        }
        }
        }
        };
Ok((new_subtree.map(Self::find_new_balance), deleted_value))
        }
        }
        }
///Helper function, remove the min value node from a subtree
///
///helper function that removes the node with min value from a
///node that has two child nodes.
///
///# Arguments
///
///* 'mut node: Box<Node<T>>' - mutable node, a Box containing Node<T>
///
///# Returns
///
///* 'Result<(Option<Box<Node<T>>>, T), ApplicationError>' - The result of operations as
///     - Successful operations return:
///        - if Some(and node has children) - node with the new subtree after the
///                   min node was removed from tree
///        - if None - Min node was leaf with no valid subtree / right children,
///                   return the data object only.
///     - On failure, returns an ApplicationError
///
fn remove_minimum(
        mut node: Box<Node<T>>,
        ) -> Result<(Option<Box<Node<T>>>, T), ApplicationError> {
        if node.left.is_none() {
    let successor = node.data;
    Ok((node.right.take(), successor))
} else {
let (new_left, minimum_value) = Self::remove_minimum(node.left.take().unwrap())?;
node.left = new_left;
Ok((Some(Self::find_new_balance(node)), minimum_value))
        }
        }
        }

 */
