import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";

export function useGuestScrollGate(triggerPx = 300) {
    const { user, loading } = useAuth();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [hasTriggered, setHasTriggered] = useState(false);

    useEffect(() => {
        if (loading || user || hasTriggered) return;

        const handleScroll = () => {
            const scrollTop = window.scrollY;
            const windowHeight = window.innerHeight;
            const docHeight = document.documentElement.scrollHeight;

            if (scrollTop + windowHeight + triggerPx >= docHeight) {
                setShowLoginModal(true);
                setHasTriggered(true);
            }
        };

        window.addEventListener("scroll", handleScroll);
        return () => window.removeEventListener("scroll", handleScroll);
    }, [user, loading, hasTriggered, triggerPx]);

    return { showLoginModal, setShowLoginModal };
}
