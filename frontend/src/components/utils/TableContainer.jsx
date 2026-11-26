import React from "react";

/**
 * The Container of the material tables: a plain div instead of a Paper. Declared once: a component written inline
 * in the components prop (Container: props => <div {...props}/>) is a new type at every render, so React mounted the
 * whole table again whenever its page rendered (a message box, a list loaded) and a row being added or edited lost
 * what had been typed.
 */
const TableContainer = props => <div {...props}/>;

export default TableContainer;
