// **********************************************************************
//
// Copyright (c) 2003-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

import Filesystem.*;

public final class DirectoryI extends PersistentDirectory
{
    public
    DirectoryI()
    {
        _destroyed = false;
        nodes = new java.util.HashMap<java.lang.String, NodeDesc>();
    }

    @Override
    public synchronized String
    name(Ice.Current current)
    {
        if(_destroyed)
        {
            throw new Ice.ObjectNotExistException(current.id, current.facet, current.operation);
        }

        return nodeName;
    }

    @Override
    public void
    destroy(Ice.Current current)
        throws PermissionDenied
    {
        if(parent == null)
        {
            throw new PermissionDenied("Cannot destroy root directory");
        }

        synchronized(this)
        {
            if(_destroyed)
            {
                throw new Ice.ObjectNotExistException(current.id, current.facet, current.operation);
            }
            if(!nodes.isEmpty())
            {
                throw new PermissionDenied("Cannot destroy non-empty directory");
            }
            _destroyed = true;
        }

        //
        // Because we use a transactional evictor, these updates are guaranteed to be atomic.
        //
        parent.removeNode(nodeName);
        _evictor.remove(current.id);
    }

    @Override
    public synchronized NodeDesc[]
    list(Ice.Current current)
    {
        if(_destroyed)
        {
            throw new Ice.ObjectNotExistException(current.id, current.facet, current.operation);
        }

        NodeDesc[] result = new NodeDesc[nodes.size()];
        int i = 0;
        java.util.Iterator<NodeDesc> p = nodes.values().iterator();
        while(p.hasNext())
        {
            result[i++] = p.next();
        }
        return result;
    }

    @Override
    public synchronized NodeDesc
    find(String name, Ice.Current current)
        throws NoSuchName
    {
        if(_destroyed)
        {
            throw new Ice.ObjectNotExistException(current.id, current.facet, current.operation);
        }

        if(!nodes.containsKey(name))
        {
            throw new NoSuchName(name);
        }

        return nodes.get(name);
    }

    @Override
    public synchronized DirectoryPrx
    createDirectory(String name, Ice.Current current)
        throws NameInUse
    {
        if(_destroyed)
        {
            throw new Ice.ObjectNotExistException(current.id, current.facet, current.operation);
        }

        if(name.length() == 0 || nodes.containsKey(name))
        {
            throw new NameInUse(name);
        }

        Ice.Identity id = Ice.Util.stringToIdentity(java.util.UUID.randomUUID().toString());
        PersistentDirectory dir = new DirectoryI();
        dir.nodeName = name;
        dir.parent = PersistentDirectoryPrxHelper.uncheckedCast(current.adapter.createProxy(current.id));
        DirectoryPrx proxy = DirectoryPrxHelper.uncheckedCast(_evictor.add(dir, id));

        NodeDesc nd = new NodeDesc();
        nd.name = name;
        nd.type = NodeType.DirType;
        nd.proxy = proxy;
        nodes.put(name, nd);

        return proxy;
    }

    @Override
    public synchronized FilePrx
    createFile(String name, Ice.Current current)
        throws NameInUse
    {
        if(_destroyed)
        {
            throw new Ice.ObjectNotExistException(current.id, current.facet, current.operation);
        }

        if(name.length() == 0 || nodes.containsKey(name))
        {
            throw new NameInUse(name);
        }

        Ice.Identity id = Ice.Util.stringToIdentity(java.util.UUID.randomUUID().toString());
        PersistentFile file = new FileI();
        file.nodeName = name;
        file.parent = PersistentDirectoryPrxHelper.uncheckedCast(current.adapter.createProxy(current.id));
        FilePrx proxy = FilePrxHelper.uncheckedCast(_evictor.add(file, id));

        NodeDesc nd = new NodeDesc();
        nd.name = name;
        nd.type = NodeType.FileType;
        nd.proxy = proxy;
        nodes.put(name, nd);

        return proxy;
    }

    @Override
    public synchronized void
    removeNode(String name, Ice.Current current)
    {
        assert(nodes.containsKey(name));
        nodes.remove(name);
    }

    public static Freeze.Evictor _evictor;
    private boolean _destroyed;
}
