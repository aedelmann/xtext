/**
 */
package org.eclipse.xtext.ui.tests.refactoring.referring;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Main</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.xtext.ui.tests.refactoring.referring.Main#getReferenced <em>Referenced</em>}</li>
 * </ul>
 *
 * @see org.eclipse.xtext.ui.tests.refactoring.referring.ReferringPackage#getMain()
 * @model
 * @generated
 */
public interface Main extends EObject
{
  /**
   * Returns the value of the '<em><b>Referenced</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.xtext.ui.tests.refactoring.referring.Reference}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Referenced</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Referenced</em>' containment reference list.
   * @see org.eclipse.xtext.ui.tests.refactoring.referring.ReferringPackage#getMain_Referenced()
   * @model containment="true"
   * @generated
   */
  EList<Reference> getReferenced();

} // Main
