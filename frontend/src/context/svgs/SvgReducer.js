const SvgReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_SVGS":
            return {
                ...state,
                svgs: action.payload
            };
        case "ADD_SVG":
            return {
                ...state,
                svgs: [...state.svgs, action.payload]
            };
        case "EDIT_SVG":
            const updatedSvg = action.payload;
            const updatedSvgs = state.svgs.map(svg => {
                if (svg.id === updatedSvg.id)
                    return updatedSvg;
                return svg;
            });
            return {
                ...state,
                svgs: updatedSvgs
            };
        case "REMOVE_SVG":
            return {
                ...state,
                svgs: state.svgs.filter(svg => svg.id !== action.payload)
            };
        default:
            return state;
    }
};

export default SvgReducer;