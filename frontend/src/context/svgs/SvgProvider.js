import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import SvgContext from "./SvgContext";
import SvgReducer from "./SvgReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl} from "../../utils/UrlBuilder";

const SvgProvider = props => {
    const initialState = {
        svgs: null
    }

    const [state, dispatch] = useReducer(SvgReducer, initialState);

    const fetchSvgs = () => {
        fetch(getBaseUrl('svg'), getGetOptions())
            .then(handleErrors)
            .then(svgs => dispatch({
                type: "FETCH_SVGS",
                payload: svgs
            }))
            .catch(err => console.log(err));
    }

    const addSvg = (svg) => {
        dispatch({
            type: "ADD_SVG",
            payload: svg
        });
    }

    const editSvg = (svg) => {
        dispatch({
            type: "EDIT_SVG",
            payload: svg
        });
    }

    const removeSvg = (id) => {
        dispatch({
            type: "REMOVE_SVG",
            payload: id
        });
    }

    return (
        <SvgContext.Provider value={{svgs: state.svgs, fetchSvgs, addSvg, editSvg, removeSvg}}>
            {props.children}
        </SvgContext.Provider>
    );
}

export default SvgProvider;