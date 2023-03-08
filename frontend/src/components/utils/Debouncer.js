export const debounce = (func, wait, immediate) => {
    let timeout;

    return (...args) => {
        let context = this;
        let later = () => {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };

        let callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}
