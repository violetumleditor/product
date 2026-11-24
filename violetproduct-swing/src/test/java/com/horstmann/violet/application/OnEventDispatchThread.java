package com.horstmann.violet.application;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs the tests of a class, and what prepares them, on the event dispatch thread, like the application. An editor part measures
 * its graph there once resized, at the same time as a test that would run on another thread.
 */
class OnEventDispatchThread implements InvocationInterceptor
{
    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable
    {
        proceed(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable
    {
        proceed(invocation);
    }

    private void proceed(Invocation<Void> invocation) throws Throwable
    {
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> {
            try
            {
                invocation.proceed();
            }
            catch (Throwable throwable)
            {
                failure.set(throwable);
            }
        });
        if (failure.get() != null)
        {
            throw failure.get();
        }
    }
}
