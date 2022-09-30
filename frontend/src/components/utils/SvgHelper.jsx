import parse from "html-react-parser";

export const parseCustomSvg = (struct, color) => {
    return parse(`<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill=${color}>${struct}</svg>`);
};