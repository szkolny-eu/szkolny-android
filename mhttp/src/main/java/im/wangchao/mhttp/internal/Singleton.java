package im.wangchao.mhttp.internal;

/**
 * <p>Description  : Singleton.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/8/25.</p>
 * <p>Time         : 上午10:37.</p>
 */
public abstract class Singleton<T> {
    private T instance;

    protected abstract T create();

    public T get(){
        if (instance == null){
            synchronized (this){
                if (instance == null){
                    instance = create();
                }
            }
        }

        return instance;
    }
}
